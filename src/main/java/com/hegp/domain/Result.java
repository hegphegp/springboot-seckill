package com.hegp.domain;

public class Result<T> {
	private int code=200;
	private String message="success";
	private T data;

	public Result() {

	}

	public Result(T data) {
		this.data = data;
	}

	public Result(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public static Result ok() {
		return new Result();
	}

	public static Result ok(Object data) {
		Result r = new Result();
		r.setData(data);
		return r;
	}

	public static Result error() {
		return new Result(500, "服务异常");
	}

	public static Result error(String msg) {
		return new Result(500, msg);
	}

	public static Result error(int code, String msg) {
		return new Result(code, msg);
	}
}