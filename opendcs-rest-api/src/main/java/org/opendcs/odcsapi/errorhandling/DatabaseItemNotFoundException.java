/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

}