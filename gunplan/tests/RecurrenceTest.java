import tr.gunplan.app.Recurrence;import java.time.*;
public final class RecurrenceTest {
 static int cases=0;static ZoneId zone=ZoneId.of("Europe/Istanbul");
 static long ts(String s){return LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli();}
 static void check(String date,String time,String repeat,int mask,int day,String after,String expected){long actual=Recurrence.next(date,time,repeat,mask,day,ts(after),zone);long want=expected.equals("none")?-1:ts(expected);if(actual!=want)throw new AssertionError(repeat+": expected "+expected+" got "+(actual<0?"none":Instant.ofEpochMilli(actual).atZone(zone)));cases++;}
 static void shifted(String start,int day,String after,String expected){long result=Recurrence.next(start,"09:00","monthly",0,day,true,ts(after),zone);if(result!=ts(expected))throw new AssertionError("shifted "+after+" expected "+expected+" got "+Instant.ofEpochMilli(result).atZone(zone));cases++;}
 public static void main(String[]args){
  check("2026-10-20","09:00","none",0,20,"2026-10-19T10:00","2026-10-20T09:00");
  check("2026-10-20","09:00","none",0,20,"2026-10-20T09:00","none");
  check("2026-10-20","09:00","daily",0,20,"2026-10-20T08:59","2026-10-20T09:00");
  check("2026-10-20","09:00","daily",0,20,"2026-10-20T09:00","2026-10-21T09:00");
  check("2026-12-31","09:00","daily",0,31,"2026-12-31T20:00","2027-01-01T09:00");
  check("2026-10-01","09:00","weekly",1,1,"2026-10-20T08:00","2026-10-26T09:00");
  check("2026-10-01","09:00","weekly",5,1,"2026-10-20T08:00","2026-10-21T09:00");
  check("2026-10-01","09:00","weekly",64,1,"2026-10-25T09:00","2026-11-01T09:00");
  check("2026-10-01","09:00","weekly",31,1,"2026-10-23T09:00","2026-10-26T09:00");
  check("2026-10-01","09:00","monthly",0,2,"2026-10-20T08:00","2026-11-02T09:00");
  check("2026-01-31","09:00","monthly",0,31,"2026-01-31T09:00","2026-02-28T09:00");
  check("2026-01-31","09:00","monthly",0,31,"2026-02-28T09:00","2026-03-31T09:00");
  check("2028-01-31","09:00","monthly",0,31,"2028-01-31T09:00","2028-02-29T09:00");
  check("2026-01-29","09:00","monthly",0,29,"2026-02-28T09:00","2026-03-29T09:00");
  check("2026-01-31","09:00","monthly",0,31,"2026-04-30T09:00","2026-05-31T09:00");
  check("2026-12-02","09:00","monthly",0,2,"2026-10-20T08:00","2026-12-02T09:00");
  check("2027-01-01","09:00","weekly",1,1,"2026-10-20T08:00","2027-01-04T09:00");
  check("2026-01-01","09:00","daily",0,1,"2026-10-20T23:00","2026-10-21T09:00");
  try{Recurrence.next("2026-10-20","09:00","weekly",0,20,ts("2026-10-20T10:00"),zone);throw new AssertionError("Empty weekdays accepted");}catch(IllegalArgumentException expected){cases++;}
  try{Recurrence.next("2026-10-20","09:00","monthly",0,32,ts("2026-10-20T10:00"),zone);throw new AssertionError("Bad month day accepted");}catch(IllegalArgumentException expected){cases++;}
  shifted("2026-01-01",31,"2026-01-01T00:00","2026-02-02T09:00");
  shifted("2026-01-01",31,"2026-02-01T10:00","2026-02-02T09:00");
  shifted("2026-01-01",31,"2026-02-02T09:00","2026-03-02T09:00");
  shifted("2026-01-01",31,"2026-03-02T09:00","2026-03-31T09:00");
  shifted("2026-01-01",1,"2026-10-31T09:00","2026-11-02T09:00");
  shifted("2026-01-01",2,"2026-10-31T09:00","2026-11-02T09:00");
  shifted("2028-01-01",31,"2028-02-01T00:00","2028-02-29T09:00");
  shifted("2026-01-01",31,"2026-12-31T09:00","2027-02-01T09:00");
  shifted("2026-02-02",31,"2026-02-01T00:00","2026-03-02T09:00");
  zone=ZoneId.of("Europe/Berlin");check("2026-03-28","02:30","daily",0,28,"2026-03-28T03:00","2026-03-29T03:30");check("2026-03-28","02:30","daily",0,28,"2026-03-29T04:00","2026-03-30T02:30");
  zone=ZoneId.of("America/New_York");check("2026-10-31","01:30","daily",0,31,"2026-11-01T03:00","2026-11-02T01:30");
  System.out.println("PASS "+cases+" calendar recurrence cases incl month-end/leap/year/weekly/DST/missed occurrences");
 }
}
