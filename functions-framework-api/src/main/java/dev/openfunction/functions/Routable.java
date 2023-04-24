package dev.openfunction.functions;

/**
 * An object that can route the specified http request to the specified function.
 */
public abstract class Routable {
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_PATCH = "PATCH";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";

    /**
     * Get the supported http methods.
     *
     * @return The supported http methods.
     */
    public String[] getMethods() {
        return new String[]{METHOD_DELETE, METHOD_GET, METHOD_PATCH, METHOD_HEAD, METHOD_PUT, METHOD_POST};
    };

    /**
     * Get the URI that will be routed.
     *
     * @return The URI that will be routed.
     */
    public String getPath(){
        return "/";
    }
}
