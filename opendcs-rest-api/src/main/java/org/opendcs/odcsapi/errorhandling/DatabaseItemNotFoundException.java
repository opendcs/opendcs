package org.opendcs.odcsapi.errorhandling;

import javax.servlet.http.HttpServletResponse;

public final class DatabaseItemNotFoundException extends WebAppException
{
  /** detailed error msg */
  private String errMessage = "";

  public DatabaseItemNotFoundException(String errMessage)
  {
    super(HttpServletResponse.SC_NOT_FOUND, errMessage);
    this.errMessage = errMessage;
  }

  public DatabaseItemNotFoundException(String errMessage, Throwable throwable)
  {
    super(HttpServletResponse.SC_NOT_FOUND, errMessage, throwable);
    this.errMessage = errMessage;
  }

  public DatabaseItemNotFoundException() { }

  @Override
  public int getStatus()
  {
    return HttpServletResponse.SC_NOT_FOUND;
  }

  @Override
  public String getErrMessage()
  {
    return errMessage;
  }

  @Override
  public void setErrMessage(String errMessage)
  {
    this.errMessage = errMessage;
  }
}