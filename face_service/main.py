"""
TeamHub Face Recognition Microservice
======================================
Library : DeepFace (TensorFlow backend) — no dlib/C++ build required
Endpoints:
  POST /enroll     { user_id: int, email: str }   — capture & save face embedding
  POST /recognize  {}                              — capture & find closest match
  GET  /health     {}                              — liveness check

Embeddings are stored as .npy files in ./embeddings/{user_id}.npy
Each file is a dict: { email, embedding }

Run:  uvicorn main:app --port 8765 --reload
"""

from __future__ import annotations

import os
import json
import time
import numpy as np
import cv2
from pathlib import Path
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from deepface import DeepFace

# ─── Config ───────────────────────────────────────────────────────────────────
EMBEDDINGS_DIR = Path(__file__).parent / "embeddings"
EMBEDDINGS_DIR.mkdir(exist_ok=True)

MODEL_NAME     = "Facenet"          # fast, accurate, ~100-dim
DISTANCE_METRIC = "cosine"
THRESHOLD      = 0.40               # cosine distance — lower = stricter

# ─── FastAPI App ──────────────────────────────────────────────────────────────
app = FastAPI(title="TeamHub Face Auth Service", version="1.0.0")

# ─── Request / Response Models ────────────────────────────────────────────────
class EnrollRequest(BaseModel):
    user_id: int
    email: str

class EnrollResponse(BaseModel):
    success: bool
    message: str

class RecognizeResponse(BaseModel):
    matched: bool
    email: str | None = None
    confidence: float | None = None   # 0.0–1.0, higher = more confident
    message: str = ""

class HealthResponse(BaseModel):
    status: str
    enrolled_users: int

# ─── Helpers ──────────────────────────────────────────────────────────────────

def capture_face_frame(window_title: str = "TeamHub Face ID", timeout_sec: int = 15) -> np.ndarray | None:
    """
    Opens the webcam, shows a preview window, and captures a frame once a face
    is detected.  Returns the frame (BGR) or None on timeout/error.
    """
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        raise RuntimeError("Cannot open webcam — check camera connection")

    face_cascade = cv2.CascadeClassifier(
        cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    )

    deadline = time.time() + timeout_sec
    captured = None

    try:
        while time.time() < deadline:
            ret, frame = cap.read()
            if not ret:
                continue

            gray  = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            faces = face_cascade.detectMultiScale(gray, 1.1, 5, minSize=(80, 80))

            # Draw rectangles around detected faces
            for (x, y, w, h) in faces:
                cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 229, 255), 2)

            # Status bar at top
            status = "Face detected — hold still..." if len(faces) > 0 else "Looking for face..."
            color  = (0, 229, 255) if len(faces) > 0 else (90, 100, 120)
            cv2.rectangle(frame, (0, 0), (frame.shape[1], 36), (8, 12, 26), -1)
            cv2.putText(frame, f"TeamHub Face ID  |  {status}",
                        (12, 24), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 1)

            cv2.imshow(window_title, frame)
            key = cv2.waitKey(30) & 0xFF
            if key == 27:   # ESC to cancel
                break

            # Auto-capture when a single face is steadily visible
            if len(faces) == 1:
                captured = frame.copy()
                # Show "captured" flash briefly
                overlay = captured.copy()
                cv2.rectangle(overlay, (0, 0), (captured.shape[1], captured.shape[0]),
                              (0, 229, 255), -1)
                cv2.addWeighted(overlay, 0.15, captured, 0.85, 0, captured)
                cv2.imshow(window_title, captured)
                cv2.waitKey(500)
                break
    finally:
        cap.release()
        cv2.destroyAllWindows()

    return captured


def get_embedding(frame: np.ndarray) -> np.ndarray:
    """Extract face embedding vector from a BGR frame using DeepFace."""
    result = DeepFace.represent(
        img_path=frame,
        model_name=MODEL_NAME,
        enforce_detection=True,
        detector_backend="opencv"
    )
    if not result:
        raise ValueError("No face embedding could be extracted")
    return np.array(result[0]["embedding"], dtype=np.float32)


def cosine_distance(a: np.ndarray, b: np.ndarray) -> float:
    """Cosine distance between two unit-normalized vectors."""
    a_n = a / (np.linalg.norm(a) + 1e-9)
    b_n = b / (np.linalg.norm(b) + 1e-9)
    return float(1.0 - np.dot(a_n, b_n))


def load_all_embeddings() -> list[dict]:
    """Load all stored embeddings from disk."""
    records = []
    for path in EMBEDDINGS_DIR.glob("*.npy"):
        data = np.load(str(path), allow_pickle=True).item()
        records.append(data)
    return records


def embedding_path(user_id: int) -> Path:
    return EMBEDDINGS_DIR / f"{user_id}.npy"


# ─── Endpoints ────────────────────────────────────────────────────────────────

@app.get("/health", response_model=HealthResponse)
def health():
    count = len(list(EMBEDDINGS_DIR.glob("*.npy")))
    return HealthResponse(status="ok", enrolled_users=count)


@app.post("/enroll", response_model=EnrollResponse)
def enroll(req: EnrollRequest):
    """
    Open webcam, capture a face, save embedding for the given user.
    """
    try:
        frame = capture_face_frame(window_title="TeamHub — Enroll Face ID")
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))

    if frame is None:
        raise HTTPException(status_code=408, detail="No face captured — timed out or cancelled")

    try:
        embedding = get_embedding(frame)
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Could not extract face: {e}")

    record = {
        "user_id":   req.user_id,
        "email":     req.email,
        "embedding": embedding,
    }
    np.save(str(embedding_path(req.user_id)), record)

    return EnrollResponse(success=True, message=f"Face enrolled for {req.email}")


@app.post("/recognize", response_model=RecognizeResponse)
def recognize():
    """
    Open webcam, capture face, compare against all stored embeddings.
    Returns the best match if within threshold.
    """
    records = load_all_embeddings()
    if not records:
        raise HTTPException(status_code=404, detail="No faces enrolled yet")

    try:
        frame = capture_face_frame(window_title="TeamHub — Sign In with Face ID")
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e))

    if frame is None:
        return RecognizeResponse(matched=False, message="Cancelled or timed out")

    try:
        query_embedding = get_embedding(frame)
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Could not extract face: {e}")

    best_dist  = float("inf")
    best_email = None

    for rec in records:
        dist = cosine_distance(query_embedding, rec["embedding"])
        if dist < best_dist:
            best_dist  = dist
            best_email = rec["email"]

    if best_dist <= THRESHOLD:
        confidence = round(1.0 - (best_dist / THRESHOLD), 4)
        return RecognizeResponse(
            matched=True,
            email=best_email,
            confidence=confidence,
            message="Face recognized"
        )

    return RecognizeResponse(
        matched=False,
        confidence=round(1.0 - (best_dist / THRESHOLD), 4),
        message="No matching face found"
    )
