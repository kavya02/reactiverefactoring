package myhomework.com.meilmanager.Utils;

/**
 * Created by root on 25.07.2016.
 */
public interface GenericListener<T> {

    public void onResponse(T Object);

    public void onError(String errorMessage);

}
