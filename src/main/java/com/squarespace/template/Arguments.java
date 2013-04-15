package com.squarespace.template;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Parses arguments for Formatter instances. Associates an argument delimiter with
 * the list of arguments, and lets Formatters make quick assertions to ensure the
 * correct number of arguments were passed in.
 */
public class Arguments {
  
  private char delimiter = ' ';
  
  private List<String> args = Constants.EMPTY_LIST_OF_STRING;

  public Arguments() {
  }
  
  public Arguments(StringView raw) {
    parse(raw);
  }
  
  private void parse(StringView raw) {
    if (raw.length() == 0) {
      return;
    }
    delimiter = raw.charAt(0);
    raw = raw.subview(1, raw.length());
    args = Arrays.asList(StringUtils.split(raw.repr(), delimiter));
  }
  
  public String first() {
    return args.get(0);
  }
  
  public String get(int index) {
    return args.get(index);
  }
  
  public int count() {
    return args.size();
  }
  
  public boolean isEmpty() {
    return args.size() == 0;
  }
  
  public char getDelimiter() {
    return delimiter;
  }
  
  public List<String> getArgs() {
    return args;
  }
  
  // Helper methods to make assertions about the args.

  public void exactly(int num) throws ArgumentsException {
    if (args.size() != num) {
      throw new ArgumentsException("Wrong number of args, exactly " + num + " expected");
    }
  }
  
  public void atMost(int num) throws ArgumentsException {
    between(0, num);
  }
  
  public void atLeast(int num) throws ArgumentsException {
    between(num, Integer.MAX_VALUE);
  }
  
  public void between(int min, int max) throws ArgumentsException {
    if (args.size() < min) {
      throw new ArgumentsException("Not enough args. At least " + min + " expected");
    }
    if (args.size() > max) {
      throw new ArgumentsException("Too many args. Takes between " + min + " and " + max);
    }
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Arguments) {
      Arguments other = (Arguments) obj;
      return delimiter == other.delimiter && args.equals(other.args);
    }
    return false;
  }

}
