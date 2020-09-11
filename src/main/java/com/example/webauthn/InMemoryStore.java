package com.example.webauthn;

import io.vertx.core.Future;
import io.vertx.ext.auth.webauthn.store.Authenticator;
import io.vertx.ext.auth.webauthn.store.AuthenticatorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryStore implements AuthenticatorStore {

  private final List<Authenticator> database;

  public InMemoryStore() {
    database = new ArrayList<>();
  }

  @Override
  public Future<List<Authenticator>> fetch(Authenticator query) {
    return Future.succeededFuture(
      database.stream()
        .filter(entry -> {
          if (query.getUserName() != null) {
            return query.getUserName().equals(entry.getUserName());
          }
          if (query.getCredID() != null) {
            return query.getCredID().equals(entry.getCredID());
          }
          // This is a bad query! both username and credID are null
          return false;
        })
        .collect(Collectors.toList())
    );
  }

  @Override
  public Future<Void> store(Authenticator authenticator) {

    long updated = database.stream()
      .filter(entry -> authenticator.getCredID().equals(entry.getCredID()))
      .peek(entry -> {
        // update existing counter
        entry.setCounter(authenticator.getCounter());
      }).count();

    if (updated > 0) {
      return Future.succeededFuture();
    } else {
      database.add(authenticator);
      return Future.succeededFuture();
    }
  }
}
