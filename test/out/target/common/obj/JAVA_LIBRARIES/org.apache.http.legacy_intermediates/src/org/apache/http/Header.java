package org.apache.http;
@java.lang.Deprecated()
public interface Header
{
public abstract  java.lang.String getName();
public abstract  java.lang.String getValue();
public abstract  org.apache.http.HeaderElement[] getElements() throws org.apache.http.ParseException;
}