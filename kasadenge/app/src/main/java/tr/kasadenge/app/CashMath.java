package tr.kasadenge.app;
public final class CashMath {
 public static final long MAX=9_000_000_000_000L;
 public static long checked(long value){if(value<0||value>MAX)throw new IllegalArgumentException("Tutar sınırı aşıldı veya negatif tutar girildi.");return value;}
 public static long add(long a,long b){return checked(Math.addExact(a,b));}
 public static long line(long value,long count){if(count<0||count>1000000||value<=0)throw new IllegalArgumentException("Para adedi veya değeri geçersiz.");return checked(Math.multiplyExact(value,count));}
 public static long[] totals(long expected,long advance,long paper,long coins,long other){checked(expected);checked(advance);checked(paper);checked(coins);checked(other);long target=add(expected,advance),counted=add(add(paper,coins),other),remaining=target-paper-coins;return new long[]{target,counted,remaining,(counted-advance)-expected,counted-advance};}
}
