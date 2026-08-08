package com.kaustack.groups.security;

public class AuthContextHolder {

    private static final ThreadLocal<AuthContext> holder = new ThreadLocal<>();

    public static void set(AuthContext ctx) {
        holder.set(ctx);
    }

    public static AuthContext get() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }
}
