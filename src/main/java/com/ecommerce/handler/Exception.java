package com.ecommerce.handler;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Exception<E> {

    private String hostName;

    private String path;

    public Date createTime;

    private E message;
}
