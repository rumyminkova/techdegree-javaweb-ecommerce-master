package com.acme.ecommerce.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Rumy on 8/25/2017.
 */
public class NotEnoughProductAvailableException extends RuntimeException{
    public NotEnoughProductAvailableException() {

        super("Not enough product in stock.");
    }
}
