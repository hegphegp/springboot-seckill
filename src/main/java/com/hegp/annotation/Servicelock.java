package com.hegp.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)    
@Documented    
public  @interface Servicelock { 
	 String description()  default "";
}
