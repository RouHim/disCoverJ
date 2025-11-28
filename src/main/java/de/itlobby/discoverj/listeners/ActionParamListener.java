package de.itlobby.discoverj.listeners;

@FunctionalInterface
public interface ActionParamListener<T> {
  void onAction(T param);
}
