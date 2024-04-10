package net.cakemine.playerservers.bungee;

import net.md_5.bungee.api.*;
import net.md_5.bungee.api.connection.*;
import java.text.*;
import java.util.concurrent.*;
import java.util.*;

public class ExpiryTracker
{
    PlayerServers pl;
    ProxyServer proxy;
    
    public ExpiryTracker(PlayerServers pl) {
        this.proxy = ProxyServer.getInstance();
        this.pl = pl;
    }
    
    public void addTime(ProxiedPlayer proxiedPlayer, int n, String s) {
        this.addTime(proxiedPlayer.getUniqueId().toString(), n, s);
    }
    
    public void addTime(String s, int n, String s2) {
        String date = this.getDate(s);
        int convertDateUnit = this.convertDateUnit(s2);
        Calendar instance = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if (date == null) {
            instance.add(convertDateUnit, n);
        }
        else if (this.msLeft(s) <= 0L) {
            instance.add(convertDateUnit, n);
        }
        else {
            try {
                instance.setTime(simpleDateFormat.parse(date));
            }
            catch (ParseException ex) {
                ex.printStackTrace();
            }
            instance.add(convertDateUnit, n);
        }
        this.pl.serverManager.setServerInfo(s, "expire-date", simpleDateFormat.format(instance.getTime()));
    }
    
    public void removeTime(ProxiedPlayer proxiedPlayer, int n, String s) {
        this.removeTime(proxiedPlayer.getUniqueId().toString(), n, s);
    }
    
    public void removeTime(String s, int n, String s2) {
        String date = this.getDate(s);
        Calendar instance = Calendar.getInstance();
        int convertDateUnit = this.convertDateUnit(s2);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            instance.setTime(simpleDateFormat.parse(date));
        }
        catch (ParseException ex) {
            ex.printStackTrace();
        }
        instance.add(convertDateUnit, -n);
        this.pl.serverManager.setServerInfo(s, "expire-date", simpleDateFormat.format(instance.getTime()));
    }
    
    public int daysLeft(ProxiedPlayer proxiedPlayer) {
        return this.daysLeft(proxiedPlayer.getUniqueId().toString());
    }
    
    public int daysLeft(String s) {
        return (int)Math.round(Math.ceil(this.msLeft(s)) / 8.64E7f);
    }
    
    public long msLeft(String s) {
        String date = this.getDate(s);
        Calendar instance = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            instance.setTime(simpleDateFormat.parse(date));
        }
        catch (ParseException ex) {
            ex.printStackTrace();
        }
        return instance.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
    }
    
    public long stringToMillis(String s) {
        if (!s.matches("(?i)(.*)(month|mnth|mnt|mth|mo|week|wk|wek|w|day|dy|d|hour|hr|h|minute|min|mi|m|sec|second)(s)?")) {
            return -1L;
        }
        int intValue = Integer.valueOf(s.replaceAll("(?i)([A-Z]|\\s|\\-)", ""));
        s.replaceAll("(?i)([0-9]|\\s|\\-)", "");
        if (s.matches("(?i)(.*)(month|mnth|mnt|mth|mo)(s)?")) {
            return TimeUnit.DAYS.toMillis(intValue * 30);
        }
        if (s.matches("(?i)(.*)(week|wk|wek|w)(s)?")) {
            return TimeUnit.DAYS.toMillis(intValue * 7);
        }
        if (s.matches("(?i)(.*)(day|dy|d)(s)?")) {
            return TimeUnit.DAYS.toMillis(intValue);
        }
        if (s.matches("(?i)(.*)(hour|hr|h)(s)?")) {
            return TimeUnit.HOURS.toMillis(intValue);
        }
        if (s.matches("(?i)(.*)(minute|min|mi|m)(s)?")) {
            return TimeUnit.MINUTES.toMillis(intValue);
        }
        if (s.matches("(?i)(.*)(sec|second(s)?)")) {
            return TimeUnit.SECONDS.toMillis(intValue);
        }
        return -1L;
    }
    
    public String timeLeft(String s) {
        return this.niceTime(this.msLeft(s));
    }
    
    public String niceTime(long n) {
        long n2 = 1000L;
        long n3 = 60L * n2;
        long n4 = 60L * n3;
        long n5 = 24L * n4;
        double n6 = 2.63E9;
        StringBuilder sb = new StringBuilder();
        if (n > n6) {
            int n7 = (int)Math.round(n / n6);
            sb.append(n7);
            if (n7 == 1) {
                sb.append(" month ");
            }
            else {
                sb.append(" months ");
            }
            n %= (long)n6;
        }
        if (n > n5) {
            int n8 = (int)Math.round(Math.ceil(n / n5));
            sb.append(n8);
            if (n8 == 1) {
                sb.append(" day ");
            }
            else {
                sb.append(" days ");
            }
            n %= n5;
        }
        if (n > n4) {
            int n9 = (int)Math.round(Math.ceil(n / n4));
            sb.append(n9);
            if (n9 == 1) {
                sb.append(" hour ");
            }
            else {
                sb.append(" hours ");
            }
            n %= n4;
        }
        if (n > n3) {
            int n10 = (int)Math.round(Math.ceil(n / n3));
            sb.append(n10);
            if (n10 == 1) {
                sb.append(" min ");
            }
            else {
                sb.append(" mins ");
            }
            n %= n3;
        }
        if (n > n2 && sb.length() < 2) {
            sb.append((int)Math.round(Math.ceil(n / n2)));
            sb.append(" seconds ");
        }
        if (sb.length() > 1) {
            if (sb.charAt(sb.length() - 1) == ' ') {
                sb.deleteCharAt(sb.length() - 1);
            }
        }
        else {
            sb.append("0 min");
        }
        return sb.toString();
    }
    
    public String expireDate(ProxiedPlayer proxiedPlayer) {
        return this.expireDate(proxiedPlayer.getUniqueId().toString());
    }
    
    public String expireDate(String s) {
        String date = this.getDate(s);
        Calendar instance = Calendar.getInstance();
        try {
            instance.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(date));
        }
        catch (ParseException ex) {
            ex.printStackTrace();
        }
        return new SimpleDateFormat("MM/dd/yyyy").format(instance.getTime());
    }
    
    public int convertDateUnit(String s) {
        int n = 5;
        if (s.matches("(?i)(month|mnth|mnt|mth|mo)(s)?")) {
            n = 2;
        }
        else if (s.matches("(?i)(week|wk|wek|w)(s)?")) {
            n = 3;
        }
        else if (s.matches("(?i)(day|dy|d)(s)?")) {
            n = 5;
        }
        else if (s.matches("(?i)(hour|hr|h)(s)?")) {
            n = 11;
        }
        else if (s.matches("(?i)(minute|min|mi|m)(s)?")) {
            n = 12;
        }
        this.pl.utils.debug("unit = " + s + " | unitConv = " + n);
        return n;
    }
    
    public boolean validUnit(String s) {
        return s.matches("(?i)(month|mnth|mnt|mth|mo|week|wk|wek|w|day|dy|d|hour|hr|h|minute|min|mi|m)(s)?");
    }
    
    public String getDate(String s) {
        if (!this.pl.serverManager.serverMap.containsKey(s) || !this.pl.serverManager.serverMap.get(s).fromHashMap().containsKey("expire-date") || this.pl.serverManager.serverMap.get(s).fromHashMap().get("expire-date").isEmpty()) {
            return "1989-04-20 16:20";
        }
        return this.pl.serverManager.serverMap.get(s).fromHashMap().get("expire-date");
    }
}
