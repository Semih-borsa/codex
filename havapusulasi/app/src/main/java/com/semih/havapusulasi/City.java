package com.semih.havapusulasi;

final class City {
  final long id;
  final String name, region, country, zone;
  final double latitude, longitude;

  City(long id, String name, String region, String country, double lat, double lon, String zone) {
    if (name == null
        || name.trim().isEmpty()
        || !Double.isFinite(lat)
        || !Double.isFinite(lon)
        || Math.abs(lat) > 90
        || Math.abs(lon) > 180) throw new IllegalArgumentException("Geçersiz şehir");
    java.time.ZoneId.of(zone);
    this.id = id;
    this.name = name;
    this.region = region;
    this.country = country;
    this.latitude = lat;
    this.longitude = lon;
    this.zone = zone;
  }

  String label() {
    return name
        + (region.isEmpty() || region.equals(name) ? "" : ", " + region)
        + (country.isEmpty() ? "" : ", " + country);
  }
}
