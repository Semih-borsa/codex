package com.semih.havapusulasi;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

public class SyncPolicyTest {
  @Test
  public void halfHourPolicyRespectsAndroidPeriodFloor() {
    assertEquals(1800000L, SyncPolicy.interval(30));
    assertEquals(300000L, SyncPolicy.flex(30));
  }

  @Test
  public void invalidPreferenceFallsBackToBoundedHalfHour() {
    assertEquals(30, SyncPolicy.minutes(-1));
    assertEquals(30, SyncPolicy.minutes(Integer.MAX_VALUE));
  }

  @Test
  public void longerChoicesHaveValidFlex() {
    for (int n : new int[] {60, 180}) {
      assertTrue(SyncPolicy.flex(n) >= 300000);
      assertTrue(SyncPolicy.flex(n) < SyncPolicy.interval(n));
    }
    assertEquals(10800000L, SyncPolicy.interval(180));
  }

  private City city(int id) {
    return new City(id, "Şehir " + id, "", "", id, id, "UTC");
  }

  @Test
  public void visibleWidgetsAndAppCityRefreshFirst() {
    List<City> list =
        RefreshJob.ordered(
            Arrays.asList(city(1), city(2), city(3)), 2, Collections.singletonList(3L));
    assertEquals(3, list.get(0).id);
    assertEquals(2, list.get(1).id);
    assertEquals(1, list.get(2).id);
  }

  @Test
  public void repeatedWidgetCitiesDoNotMultiplyRequests() {
    List<City> list =
        RefreshJob.ordered(Arrays.asList(city(1), city(2)), 1, Arrays.asList(2L, 2L, 1L));
    assertEquals(2, list.size());
  }

  @Test
  public void deletedWidgetCityDoesNotReplaceValidSavedCities() {
    List<City> list = RefreshJob.ordered(Arrays.asList(city(1), city(2)), 0, Arrays.asList(77L));
    assertEquals(2, list.size());
    assertEquals(1, list.get(0).id);
  }
}
