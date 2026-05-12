-- Test Data for Trending Feed & Leaderboard
-- Run this script to populate the database with realistic test data
-- NOTE: The migration.sql script already inserts test users.
-- Run this only if you skipped migration.sql or want to reset user data.

-- ========================================
-- 1. USERS (5 users with varying activity levels)
--    Passwords are BCrypt-hashed. All test users use: password123
--    Hash generated with BCrypt cost 12
-- ========================================
INSERT INTO users (id, username, email, password_hash, role, status, email_verified, profile_photo) VALUES
(1, 'GhostProtocol', 'ghost@example.com', '$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq', 'PLAYER', 'ACTIVE', TRUE, 'uploads/profiles/default.png'),
(2, 'NeonSniper', 'neon@example.com', '$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq', 'PLAYER', 'ACTIVE', TRUE, 'uploads/profiles/default.png'),
(3, 'PixelQueen', 'pixel@example.com', '$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq', 'PLAYER', 'ACTIVE', TRUE, 'uploads/profiles/default.png'),
(4, 'ShadowBlade', 'shadow@example.com', '$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq', 'PLAYER', 'ACTIVE', TRUE, 'uploads/profiles/default.png'),
(5, 'CyberWolf', 'cyber@example.com', '$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq', 'PLAYER', 'ACTIVE', TRUE, 'uploads/profiles/default.png');

-- ========================================
-- 2. POSTS (15 posts across different users and timestamps)
-- ========================================

-- User 1 (GhostProtocol) - Very active user, 4 posts
INSERT INTO posts (id, user_id, username, content, game_tag, created_at) VALUES
(1, 1, 'GhostProtocol', 'Just hit Diamond rank after 200 hours! The grind was real but totally worth it. Pro tip: focus on crosshair placement and utility usage, not just aim.', 'VALORANT', DATE_SUB(NOW(), INTERVAL 2 HOUR)),

(2, 1, 'GhostProtocol', 'Looking for a duo to climb to Ascendant. I main Jett/Reyna, need a controller or initiator. DM me if interested!', 'VALORANT', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

(3, 1, 'GhostProtocol', 'The new patch completely changed the meta. Jett got nerfed hard, time to switch to Chamber!', 'VALORANT', DATE_SUB(NOW(), INTERVAL 5 HOUR)),

(4, 1, 'GhostProtocol', 'Stream is LIVE! Road to Radiant starts NOW! Come hang out and watch some insane plays.', 'General', DATE_SUB(NOW(), INTERVAL 1 HOUR));

-- User 2 (NeonSniper) - Active user, 3 posts
INSERT INTO posts (id, user_id, username, content, game_tag, created_at) VALUES
(5, 2, 'NeonSniper', 'My new gaming setup is finally complete! RTX 4080, 240Hz monitor, and custom RGB lighting. Rate it 1-10!', 'General', DATE_SUB(NOW(), INTERVAL 3 HOUR)),

(6, 2, 'NeonSniper', 'CS2 is broken after the latest update. Getting 100 FPS drops every match. Anyone else experiencing this?', 'CS2', DATE_SUB(NOW(), INTERVAL 1 DAY)),

(7, 2, 'NeonSniper', 'Just pulled off an ACE clutch with only a Deagle. My hands are still shaking!', 'CS2', DATE_SUB(NOW(), INTERVAL 45 MINUTE));

-- User 3 (PixelQueen) - Very active, 4 posts
INSERT INTO posts (id, user_id, username, content, game_tag, created_at) VALUES
(8, 3, 'PixelQueen', 'Started my League of Legends journey last month and already reached Gold! Here are my top 5 tips for new players.', 'League of Legends', DATE_SUB(NOW(), INTERVAL 4 HOUR)),

(9, 3, 'PixelQueen', 'Who else thinks the new champion is completely broken? Her abilities have zero counterplay!', 'League of Legends', DATE_SUB(NOW(), INTERVAL 1 HOUR)),

(10, 3, 'PixelQueen', 'Hosting a custom game tonight at 9 PM EST. DM for invite code, all ranks welcome!', 'League of Legends', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),

(11, 3, 'PixelQueen', 'My first pentakill! The team went so hype in voice chat, best moment ever!', 'League of Legends', DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- User 4 (ShadowBlade) - Moderate activity, 2 posts
INSERT INTO posts (id, user_id, username, content, game_tag, created_at) VALUES
(12, 4, 'ShadowBlade', 'Rainbow Six Siege new operator is insane! The ability to see through walls for 5 seconds is game-changing.', 'Rainbow Six Siege', DATE_SUB(NOW(), INTERVAL 6 HOUR)),

(13, 4, 'ShadowBlade', 'Looking for a ranked team, I play Attack main. Currently Platinum 2. Add me!', 'Rainbow Six Siege', DATE_SUB(NOW(), INTERVAL 12 HOUR));

-- User 5 (CyberWolf) - Casual, 2 posts
INSERT INTO posts (id, user_id, username, content, game_tag, created_at) VALUES
(14, 5, 'CyberWolf', 'Just won my first Fortnite tournament! Never thought I would actually win something. Dreams do come true!', 'Fortnite', DATE_SUB(NOW(), INTERVAL 1 DAY)),

(15, 5, 'CyberWolf', 'Anyone want to play some casual matches? I am more of a chill player, no sweat mode.', 'Fortnite', DATE_SUB(NOW(), INTERVAL 8 HOUR));

-- ========================================
-- 3. LIKES (varying engagement per post)
-- ========================================

-- Post 1 (GhostProtocol - Diamond rank) - High engagement: 15 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(1, 2, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 3, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 4, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 5, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(1, 1, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(1, 2, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(1, 3, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(1, 4, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 5, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(1, 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(1, 2, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(1, 3, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(1, 4, DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(1, 5, DATE_SUB(NOW(), INTERVAL 3 MINUTE)),
(1, 1, DATE_SUB(NOW(), INTERVAL 1 MINUTE));

-- Post 2 (GhostProtocol - Duo looking) - Medium engagement: 5 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(2, 3, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(2, 4, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(2, 5, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(2, 1, DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(2, 2, DATE_SUB(NOW(), INTERVAL 2 MINUTE));

-- Post 3 (GhostProtocol - Meta change) - Low engagement: 3 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(3, 4, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(3, 5, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(3, 2, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- Post 4 (GhostProtocol - Stream) - Medium engagement: 7 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(4, 2, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(4, 3, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(4, 4, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(4, 5, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(4, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(4, 2, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(4, 3, DATE_SUB(NOW(), INTERVAL 20 MINUTE));

-- Post 5 (NeonSniper - Setup) - High engagement: 12 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(5, 1, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(5, 3, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(5, 4, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5, 5, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5, 1, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(5, 2, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(5, 3, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(5, 4, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(5, 5, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(5, 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(5, 2, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(5, 3, DATE_SUB(NOW(), INTERVAL 10 MINUTE));

-- Post 6 (NeonSniper - CS2 bug) - Low engagement: 4 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(6, 1, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(6, 3, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(6, 4, DATE_SUB(NOW(), INTERVAL 15 HOUR)),
(6, 5, DATE_SUB(NOW(), INTERVAL 12 HOUR));

-- Post 7 (NeonSniper - ACE clutch) - High engagement: 18 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(7, 1, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(7, 3, DATE_SUB(NOW(), INTERVAL 38 MINUTE)),
(7, 4, DATE_SUB(NOW(), INTERVAL 36 MINUTE)),
(7, 5, DATE_SUB(NOW(), INTERVAL 34 MINUTE)),
(7, 2, DATE_SUB(NOW(), INTERVAL 32 MINUTE)),
(7, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(7, 3, DATE_SUB(NOW(), INTERVAL 28 MINUTE)),
(7, 4, DATE_SUB(NOW(), INTERVAL 26 MINUTE)),
(7, 5, DATE_SUB(NOW(), INTERVAL 24 MINUTE)),
(7, 2, DATE_SUB(NOW(), INTERVAL 22 MINUTE)),
(7, 1, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(7, 3, DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(7, 4, DATE_SUB(NOW(), INTERVAL 16 MINUTE)),
(7, 5, DATE_SUB(NOW(), INTERVAL 14 MINUTE)),
(7, 2, DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(7, 1, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(7, 3, DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(7, 4, DATE_SUB(NOW(), INTERVAL 6 MINUTE));

-- Post 8 (PixelQueen - LoL tips) - Medium engagement: 8 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(8, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(8, 2, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(8, 4, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(8, 5, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(8, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(8, 2, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(8, 3, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(8, 4, DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- Post 9 (PixelQueen - Broken champion) - High engagement: 14 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(9, 1, DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
(9, 2, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(9, 4, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(9, 5, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(9, 1, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(9, 2, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(9, 4, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(9, 5, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(9, 1, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(9, 2, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(9, 4, DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(9, 5, DATE_SUB(NOW(), INTERVAL 6 MINUTE)),
(9, 1, DATE_SUB(NOW(), INTERVAL 4 MINUTE)),
(9, 2, DATE_SUB(NOW(), INTERVAL 2 MINUTE));

-- Post 10 (PixelQueen - Custom game) - Low engagement: 4 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(10, 1, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(10, 2, DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(10, 4, DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(10, 5, DATE_SUB(NOW(), INTERVAL 5 MINUTE));

-- Post 11 (PixelQueen - Pentakill) - High engagement: 16 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(11, 1, DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(11, 2, DATE_SUB(NOW(), INTERVAL 85 MINUTE)),
(11, 4, DATE_SUB(NOW(), INTERVAL 80 MINUTE)),
(11, 5, DATE_SUB(NOW(), INTERVAL 75 MINUTE)),
(11, 1, DATE_SUB(NOW(), INTERVAL 70 MINUTE)),
(11, 2, DATE_SUB(NOW(), INTERVAL 65 MINUTE)),
(11, 4, DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
(11, 5, DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
(11, 1, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(11, 2, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(11, 4, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(11, 5, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(11, 1, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(11, 2, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(11, 4, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(11, 5, DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- Post 12 (ShadowBlade - R6 operator) - Medium engagement: 6 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(12, 1, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(12, 2, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(12, 3, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(12, 5, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(12, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(12, 2, DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- Post 13 (ShadowBlade - Team looking) - Low engagement: 2 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(13, 1, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(13, 3, DATE_SUB(NOW(), INTERVAL 8 HOUR));

-- Post 14 (CyberWolf - Tournament win) - High engagement: 10 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(14, 1, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(14, 2, DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(14, 3, DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(14, 4, DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(14, 1, DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(14, 2, DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(14, 3, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(14, 4, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(14, 1, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(14, 2, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- Post 15 (CyberWolf - Casual) - Low engagement: 2 likes
INSERT INTO likes (post_id, user_id, created_at) VALUES
(15, 1, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(15, 3, DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- ========================================
-- 4. COMMENTS (engagement across posts)
-- ========================================

-- Post 1 (GhostProtocol - Diamond rank) - 8 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(1, 2, 'Congrats man! What was your secret to climbing so fast?', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 3, 'Diamond is so hard to reach, respect!', DATE_SUB(NOW(), INTERVAL 55 MINUTE)),
(1, 4, 'I have been stuck in Gold for months, any tips?', DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(1, 5, 'Crosshair placement is everything, good advice!', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(1, 2, 'What rank did you start at this season?', DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(1, 3, 'Nice! I main Sage myself, very different playstyle.', DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(1, 4, 'The grind is real but you made it!', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 5, 'Time to push for Immortal next!', DATE_SUB(NOW(), INTERVAL 25 MINUTE));

-- Post 2 (GhostProtocol - Duo) - 3 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(2, 3, 'I can play Omen, adding you now!', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(2, 4, 'What rank are you currently?', DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(2, 5, 'Sent you a friend request!', DATE_SUB(NOW(), INTERVAL 10 MINUTE));

-- Post 3 (GhostProtocol - Meta) - 2 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(3, 4, 'Chamber is way better anyway, his traps are broken.', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(3, 5, 'I still think Jett is fine, just need better aim.', DATE_SUB(NOW(), INTERVAL 3 HOUR));

-- Post 4 (GhostProtocol - Stream) - 5 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(4, 2, 'Coming in! Love watching your streams.', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(4, 3, 'What game are you playing today?', DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(4, 4, 'Is there a chat link?', DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(4, 5, 'Just joined, you got this!', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(4, 1, 'Thanks everyone for tuning in!', DATE_SUB(NOW(), INTERVAL 25 MINUTE));

-- Post 5 (NeonSniper - Setup) - 6 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(5, 1, 'That setup looks amazing! 10/10 easily.', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(5, 3, 'What monitor is that? Looks so smooth.', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5, 4, 'The RGB is tasteful, not overdone.', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(5, 5, 'How much did the whole build cost?', DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(5, 1, 'Is that a custom loop or AIO?', DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(5, 2, 'AIO, keeping it simple and reliable!', DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- Post 6 (NeonSniper - CS2 bug) - 3 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(6, 1, 'Yeah, I am getting the same issue. So frustrating.', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(6, 3, 'Try lowering your settings, helped for me.', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(6, 4, 'Valve needs to fix this ASAP.', DATE_SUB(NOW(), INTERVAL 15 HOUR));

-- Post 7 (NeonSniper - ACE clutch) - 9 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(7, 1, 'THAT IS INSANE! Do you have a clip?', DATE_SUB(NOW(), INTERVAL 38 MINUTE)),
(7, 3, 'Deagle ace is the hardest one, massive respect.', DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(7, 4, 'Your aim must be on point today!', DATE_SUB(NOW(), INTERVAL 32 MINUTE)),
(7, 5, 'I could never do that, even with an AWP.', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(7, 1, 'What round was that? Eco round?', DATE_SUB(NOW(), INTERVAL 28 MINUTE)),
(7, 2, 'Full buy actually, makes it even crazier.', DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(7, 3, 'Please save that clip, it is legendary.', DATE_SUB(NOW(), INTERVAL 22 MINUTE)),
(7, 4, 'I would have posted it on Reddit immediately!', DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(7, 5, 'GGs, your team must have hyped so hard.', DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- Post 8 (PixelQueen - LoL tips) - 5 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(8, 1, 'What role do you main in LoL?', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(8, 2, 'I started last week, these tips are gold!', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(8, 3, 'Ward vision is so underrated, glad you mentioned it.', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(8, 4, 'I am still Iron, help me please.', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(8, 5, 'Gold in one month is impressive!', DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- Post 9 (PixelQueen - Broken champion) - 7 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(9, 1, 'Which champion? I have not played in a few days.', DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(9, 2, 'She got buffed again? This is the third time.', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(9, 4, 'Just ban her, problem solved.', DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(9, 5, 'Counter pick works too, she is not that bad.', DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(9, 1, 'Riot needs to learn game balance.', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(9, 3, 'I love playing her though, feels so powerful.', DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(9, 2, 'That is because she IS overpowered lol.', DATE_SUB(NOW(), INTERVAL 20 MINUTE));

-- Post 10 (PixelQueen - Custom game) - 2 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(10, 1, 'Count me in! Sending DM.', DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(10, 4, 'What role can I play?', DATE_SUB(NOW(), INTERVAL 10 MINUTE));

-- Post 11 (PixelQueen - Pentakill) - 8 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(11, 1, 'PENTAKILL! What champion?', DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(11, 2, 'First pentakill is always the best feeling!', DATE_SUB(NOW(), INTERVAL 85 MINUTE)),
(11, 4, 'Was it a teamfight or steal?', DATE_SUB(NOW(), INTERVAL 80 MINUTE)),
(11, 5, 'I have been playing for years and still no pentakill.', DATE_SUB(NOW(), INTERVAL 75 MINUTE)),
(11, 1, 'You are so lucky!', DATE_SUB(NOW(), INTERVAL 70 MINUTE)),
(11, 3, 'Practice and positioning, it will come!', DATE_SUB(NOW(), INTERVAL 65 MINUTE)),
(11, 2, 'Clip it and save it!', DATE_SUB(NOW(), INTERVAL 60 MINUTE)),
(11, 4, 'This deserves a celebration post!', DATE_SUB(NOW(), INTERVAL 55 MINUTE));

-- Post 12 (ShadowBlade - R6 operator) - 4 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(12, 1, 'That ability needs a nerf ASAP.', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(12, 2, 'I love it, makes the game more interesting.', DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(12, 3, 'Counter with smoke, easy.', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(12, 5, '5 seconds is too long, should be 3.', DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- Post 13 (ShadowBlade - Team) - 1 comment
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(13, 1, 'What rank do you need?', DATE_SUB(NOW(), INTERVAL 10 HOUR));

-- Post 14 (CyberWolf - Tournament) - 5 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(14, 1, 'Congrats! What tournament was it?', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(14, 2, 'First win is always special!', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(14, 3, 'What was the prize pool?', DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(14, 4, 'Keep grinding, more wins to come!', DATE_SUB(NOW(), INTERVAL 14 HOUR)),
(14, 5, 'You deserve it, man!', DATE_SUB(NOW(), INTERVAL 12 HOUR));

-- Post 15 (CyberWolf - Casual) - 2 comments
INSERT INTO comments (post_id, user_id, comment_text, created_at) VALUES
(15, 1, 'Chill games are the best sometimes.', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(15, 3, 'Adding you, I am also a casual player.', DATE_SUB(NOW(), INTERVAL 4 HOUR));

-- ========================================
-- 5. SHARES (viral content indicators)
-- ========================================

-- Post 1 (GhostProtocol - Diamond rank) - 5 shares (viral)
INSERT INTO shares (original_post_id, user_id, share_text, created_at) VALUES
(1, 2, 'This guy just hit Diamond! Inspiring stuff!', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 3, 'Diamond rank achieved! Check out his tips!', DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(1, 4, 'Road to Diamond, following this advice!', DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(1, 5, 'Sharing this for everyone to see!', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 2, 'Everyone should read this!', DATE_SUB(NOW(), INTERVAL 20 MINUTE));

-- Post 4 (GhostProtocol - Stream) - 3 shares
INSERT INTO shares (original_post_id, user_id, share_text, created_at) VALUES
(4, 3, 'Stream is LIVE, come watch!', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(4, 4, 'Going live now, join the fun!', DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(4, 5, 'Best streamer around, check it out!', DATE_SUB(NOW(), INTERVAL 35 MINUTE));

-- Post 5 (NeonSniper - Setup) - 4 shares
INSERT INTO shares (original_post_id, user_id, share_text, created_at) VALUES
(5, 1, 'This setup is goals!', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(5, 3, 'Dream setup right here!', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5, 4, 'Rate this setup 1-10, I say 10!', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(5, 5, 'Sharing for setup inspiration!', DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- Post 7 (NeonSniper - ACE clutch) - 6 shares (most viral)
INSERT INTO shares (original_post_id, user_id, share_text, created_at) VALUES
(7, 1, 'ACE with Deagle! This is insane!', DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(7, 3, 'One of the best plays I have seen!', DATE_SUB(NOW(), INTERVAL 38 MINUTE)),
(7, 4, 'Share this legendary moment!', DATE_SUB(NOW(), INTERVAL 36 MINUTE)),
(7, 5, 'Deagle ACE, never gets old!', DATE_SUB(NOW(), INTERVAL 34 MINUTE)),
(7, 1, 'Everyone needs to see this!', DATE_SUB(NOW(), INTERVAL 32 MINUTE)),
(7, 2, 'Sharing my best moment ever!', DATE_SUB(NOW(), INTERVAL 30 MINUTE));

-- Post 9 (PixelQueen - Broken champion) - 3 shares
INSERT INTO shares (original_post_id, user_id, share_text, created_at) VALUES
(9, 1, 'This champion is so broken, read this!', DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(9, 2, 'Agreed, needs immediate nerf!', DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(9, 4, 'Sharing for the community awareness!', DATE_SUB(NOW(), INTERVAL 40 MINUTE));

-- Post 11 (PixelQueen - Pentakill) - 4 shares
INSERT INTO shares (original_post_id, user_id, share_text, created_at) VALUES
(11, 1, 'First pentakill! Amazing!', DATE_SUB(NOW(), INTERVAL 90 MINUTE)),
(11, 2, 'Pentakill deserves recognition!', DATE_SUB(NOW(), INTERVAL 85 MINUTE)),
(11, 4, 'Sharing this epic moment!', DATE_SUB(NOW(), INTERVAL 80 MINUTE)),
(11, 5, 'Pentakill! Congrats!', DATE_SUB(NOW(), INTERVAL 75 MINUTE));

-- Post 14 (CyberWolf - Tournament) - 2 shares
INSERT INTO shares (original_post_id, user_id, share_text, created_at) VALUES
(14, 1, 'Tournament winner! Well deserved!', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(14, 3, 'First tournament win is always special!', DATE_SUB(NOW(), INTERVAL 18 HOUR));
