package tn.esprit.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodCreateParams;

public class StripeService {

    public StripeService() {
        String key = System.getenv("STRIPE_SECRET_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set the STRIPE_SECRET_KEY environment variable before running.");
        }
        Stripe.apiKey = key;
    }

    public PaymentIntent charge(long amountInCents, String currency,
                                String cardNumber, int expMonth, int expYear, String cvc)
            throws StripeException {

        PaymentMethod paymentMethod = PaymentMethod.create(
                PaymentMethodCreateParams.builder()
                        .setType(PaymentMethodCreateParams.Type.CARD)
                        .setCard(PaymentMethodCreateParams.CardDetails.builder()
                                .setNumber(cardNumber)
                                .setExpMonth((long) expMonth)
                                .setExpYear((long) expYear)
                                .setCvc(cvc)
                                .build())
                        .build()
        );

        PaymentIntent intent = PaymentIntent.create(
                PaymentIntentCreateParams.builder()
                        .setAmount(amountInCents)
                        .setCurrency(currency)
                        .setPaymentMethod(paymentMethod.getId())
                        .setConfirm(true)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setAllowRedirects(
                                                PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER
                                        )
                                        .setEnabled(true)
                                        .build()
                        )
                        .build()
        );

        return intent;
    }
}
