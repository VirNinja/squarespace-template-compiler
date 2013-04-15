package com.squarespace.template;


/**
 * Common base class for Formatters.
 */
public abstract class BaseFormatter implements Formatter {

  private String identifier;
  
  private boolean requiresArgs;
  
  public BaseFormatter(String identifier, boolean requiresArgs) {
    this.identifier = identifier;
    this.requiresArgs = requiresArgs;
  }
  
  public String getIdentifier() {
    return identifier;
  }
  
  public boolean requiresArgs() {
    return requiresArgs;
  }
  
  public void validateArgs(Arguments args) throws ArgumentsException {
  }

  public void apply(Context ctx, Arguments args) throws CodeExecuteException {
    // NOOP
  }

  @Override
  public String toString() {
    return identifier;
  }

}
