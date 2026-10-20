package com.semih.izin360;

import static org.junit.Assert.*;

import org.junit.Test;

public class EndpointTest {
  @Test
  public void acceptsPrivateWifiAddress() {
    assertEquals("http://192.168.1.20:4310", Endpoint.validate("http://192.168.1.20:4310/"));
    assertTrue(Endpoint.local("10.1.2.3"));
    assertTrue(Endpoint.local("172.31.0.1"));
  }

  @Test
  public void acceptsTrustedHttps() {
    assertEquals("https://example.com", Endpoint.validate("https://example.com/"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsPlainInternetHttp() {
    Endpoint.validate("http://example.com");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsPrivateLookingPublicDomain() {
    Endpoint.validate("http://192.168.1.20.attacker.example");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsInlineCredentials() {
    Endpoint.validate("https://user:password@example.com");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsQueryToken() {
    Endpoint.validate("https://example.com?token=not-real");
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsNonRootPath() {
    Endpoint.validate("https://example.com/app");
  }

  @Test
  public void validatesPrivateOctetsAndBoundaries() {
    assertFalse(Endpoint.local("192.168.999.1"));
    assertFalse(Endpoint.local("172.32.0.1"));
    assertFalse(Endpoint.local("8.8.8.8"));
    assertFalse(Endpoint.local("169.254.169.254"));
  }
}
