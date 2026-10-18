package com.semih.nova;

final class Conversation {
  enum State {
    OFF,
    LISTENING,
    TRANSCRIBING,
    THINKING,
    SPEAKING
  }

  private State state = State.OFF;
  private int session;

  int start() {
    session++;
    state = State.LISTENING;
    return session;
  }

  void stop() {
    session++;
    state = State.OFF;
  }

  boolean active() {
    return state != State.OFF;
  }

  int session() {
    return session;
  }

  State state() {
    return state;
  }

  boolean move(int token, State next) {
    if (!active() || token != session) return false;
    state = next;
    return true;
  }

  boolean canListen(int token) {
    return active() && session == token && state == State.SPEAKING;
  }
}
