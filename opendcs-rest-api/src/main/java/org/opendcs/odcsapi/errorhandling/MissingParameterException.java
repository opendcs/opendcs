package org.opendcs.odcsapi.errorhandling;

import javax.servlet.http.HttpServletResponse;

public final class MissingParameterException extends WebAppException
{
  /** detailed error msg */
  private String errMessage = "";

  public MissingParameterException(String errMessage)
  {
    super(HttpServletResponse.SC_BAD_REQUEST, errMessage);
    this.errMessage = errMessage;
  }

  public MissingParameterException(String errMessage, Throwable throwable)
  {
    super(HttpServletResponse.SC_BAD_REQUEST, errMessage, throwable);
    this.errMessage = errMessage;
  }

  public MissingParameterException() { }

  @Override
  public int getStatus()
  {
    return HttpServletResponse.SC_BAD_REQUEST;
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