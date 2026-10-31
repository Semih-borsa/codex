package tr.gunplan.app;
import java.time.*;
public final class Recurrence {
 public static long next(String start,String time,String repeat,int weekMask,int monthDay,long after,ZoneId zone){
  return next(start,time,repeat,weekMask,monthDay,false,after,zone);
 }
 public static long next(String start,String time,String repeat,int weekMask,int monthDay,boolean nextWeekday,long after,ZoneId zone){
  LocalDate anchor=LocalDate.parse(start);LocalTime clock=LocalTime.parse(time);LocalDate cursor=Instant.ofEpochMilli(after).atZone(zone).toLocalDate();if(cursor.isBefore(anchor))cursor=anchor;
  if("none".equals(repeat)){long t=anchor.atTime(clock).atZone(zone).toInstant().toEpochMilli();return t>after?t:-1;}
  if("monthly".equals(repeat)){
   if(monthDay<1||monthDay>31)throw new IllegalArgumentException("Ay günü 1–31 olmalı.");
   YearMonth month=YearMonth.from(cursor).minusMonths(1);for(int i=0;i<2400;i++,month=month.plusMonths(1)){LocalDate d=month.atDay(Math.min(monthDay,month.lengthOfMonth()));if(d.isBefore(anchor))continue;if(nextWeekday){while(d.getDayOfWeek()==DayOfWeek.SATURDAY||d.getDayOfWeek()==DayOfWeek.SUNDAY)d=d.plusDays(1);}long t=d.atTime(clock).atZone(zone).toInstant().toEpochMilli();if(t>after)return t;}
  }else if("daily".equals(repeat)||"weekly".equals(repeat)){
   if("weekly".equals(repeat)&&(weekMask<1||weekMask>127))throw new IllegalArgumentException("En az bir hafta günü seçin.");
   for(int i=0;i<370;i++,cursor=cursor.plusDays(1)){if("weekly".equals(repeat)&&(weekMask&(1<<(cursor.getDayOfWeek().getValue()-1)))==0)continue;long t=cursor.atTime(clock).atZone(zone).toInstant().toEpochMilli();if(t>after)return t;}
  }else throw new IllegalArgumentException("Tekrar kuralı geçersiz.");
  throw new IllegalArgumentException("Sonraki tarih hesaplanamadı.");
 }
}
