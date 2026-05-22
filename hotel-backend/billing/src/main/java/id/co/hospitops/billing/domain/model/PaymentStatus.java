package id.co.hospitops.billing.domain.model;

import org.hibernate.annotations.Struct;

@Struct(name = "payment_status")
public enum PaymentStatus {UNPAID, PARTIAL, PAID}
