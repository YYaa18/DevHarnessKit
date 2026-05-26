package com.acme.legacy.order.web;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface LegacyRoute {
    String method();
    String path();
}
