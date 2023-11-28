package com.utility.mobile.bluetoothle.utils;

import android.util.Log;

/**
 * Created by thomaspethtel on 2/24/17.
 *
 */
public class SoftwareVersionString implements Comparable<SoftwareVersionString> {

    private static final String TAG = "SoftwareVersionString";

    private String[] values;

    private SoftwareVersionString(String cleanVersion){
        values = cleanVersion.split("\\.");
    }

    public SoftwareVersionString(String majorVersion, String minorVersion, String revision){
        Log.d(TAG, "majorVersion: " + majorVersion + " minorVersion: " + minorVersion + " revision: " + revision);
        values = new String[]{majorVersion, minorVersion, revision};
    }

    public static SoftwareVersionString fromString(String string){
        String stripDebug = string.substring(0, string.contains(" ") ? string.indexOf(" ") : string.length());
        return new SoftwareVersionString(stripDebug);
    }

    public static SoftwareVersionString fromFile(String filename){
//        String re1=".*?";	// Non-greedy match on filler
//        String re2="(\\d+)";	// Integer Number 1
//        String re3="(_)";	// Any Single Character 1
//        String re4="(\\d+)";	// Integer Number 2
//        String re5="(_)";	// Any Single Character 2
//        String re6="(\\d+)";	// Integer Number 3
//
//        Pattern p = Pattern.compile(re1+re2+re3+re4+re5+re6,Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
//        Matcher m = p.matcher(filename);
//        if (m.find()) {
//            String majorVersion=m.group(1);
//            String minorVersion=m.group(3);
//            String revision=m.group(5);
//            return new SoftwareVersionString(majorVersion, minorVersion, revision);
//        }

        if(filename != null){
            String [] filenameSplit = filename.split("_");
            for(int i = 0; i <filenameSplit.length; i++ ){
                Log.d(TAG,"index[" + i + "]: " + filenameSplit[i]);
            }
            String majorVersion = filenameSplit[filenameSplit.length - 3];
            String minorVersion = filenameSplit[filenameSplit.length - 2];
            String revision = filenameSplit[filenameSplit.length - 1].split("\\.")[0];

            return new SoftwareVersionString(majorVersion, minorVersion, revision);
        }
        return null;
    }

    @Override
    public int compareTo(SoftwareVersionString another) {
        int i = 0;
        // set index to first non-equal ordinal or length of shortest version string
        while (i < this.values.length && i < another.values.length && this.values[i].equals(another.values[i])) {
            i++;
        }
        // compare first non-equal ordinal number
        if (i < this.values.length && i < another.values.length) {
            int diff = Integer.valueOf(this.values[i]).compareTo(Integer.valueOf(another.values[i]));
            return Integer.signum(diff);
        }
        // the strings are equal or one string is a substring of the other
        // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
        return Integer.signum(this.values.length - another.values.length);
    }

    @Override
    public String toString() {
        String version = "";
        if (values != null && values.length > 0) {
            for (String value : values) {
                version = version + value + ".";
            }
            version = version.substring(0, version.length() - 1);
        }
        return version;
    }
}
