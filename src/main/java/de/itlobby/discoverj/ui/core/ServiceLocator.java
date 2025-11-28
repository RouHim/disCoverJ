package de.itlobby.discoverj.ui.core;

import java.util.HashMap;

public class ServiceLocator {

  private static ServiceLocator instance;
  private final HashMap<Class, Object> services = new HashMap<>();

  public static <T> T get(Class<T> clazz) {
    return get(clazz, false);
  }

  public static <T> void unload(Class<T> clazz) {
    if (instance == null) {
      instance = new ServiceLocator();
    }

    instance.unloadInternal(clazz);
  }

  public static <T> T get(Class<T> clazz, boolean requireNew) {
    if (instance == null) {
      instance = new ServiceLocator();
    }

    return instance.getService(clazz, requireNew);
  }

  private <T> void unloadInternal(Class<T> clazz) {
    services.remove(clazz);
  }

  private <T> T getService(Class<T> clazz, boolean requireNew) {
    if (requireNew) {
      return createServiceInstance(clazz);
    } else {
      return getService(clazz);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getService(Class<T> clazz) {
    T obj = (T) services.get(clazz);

    if (obj == null) {
      obj = createServiceInstance(clazz);
    }

    return obj;
  }

  @SuppressWarnings({ "squid:S00112", "squid:S1166" })
  private <T> T createServiceInstance(Class<T> clazz) {
    T obj;

    try {
      obj = clazz.getDeclaredConstructor().newInstance();
      services.put(clazz, obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return obj;
  }
}
