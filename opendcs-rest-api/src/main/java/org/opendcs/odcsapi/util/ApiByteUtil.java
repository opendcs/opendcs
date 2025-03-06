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

/*
*  $Id: ApiByteUtil.java,v 1.2 2022/12/06 13:45:51 mmaloney Exp $
*
*  $Source: /home/cvs/repo/odcsapi/src/main/java/org/opendcs/odcsapi/util/ApiByteUtil.java,v $
*
*  $State: Exp $
*
*  $Log: ApiByteUtil.java,v $
*  Revision 1.2  2022/12/06 13:45:51  mmaloney
*  Refactor to stop using ilex.util.Logger and start using java.util.logging.
*
*  Revision 1.1  2022/11/29 15:05:13  mmaloney
*  First cut of refactored DAOs and beans to remove dependency on opendcs.jar
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.4  2011/09/10 10:23:34  mmaloney
*  added getFloat method
*
*  Revision 1.3  2009/10/30 15:31:25  mjmaloney
*  Added toHexAsciiString -- useful for debugging output.
*
*  Revision 1.2  2008/09/05 12:53:53  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.11  2004/08/30 14:50:24  mjmaloney
*  Javadocs
*
*  Revision 1.10  2003/12/20 00:32:50  mjmaloney
*  Implemented TimeoutInputStream.
*
*  Revision 1.9  2003/08/19 15:51:37  mjmaloney
*  dev
*
*  Revision 1.8  2003/05/16 20:12:38  mjmaloney
*  Added EnvExpander. This is preferrable to ShellExpander because
*  it is platform independent.
*
*  Revision 1.7  2003/04/09 15:16:11  mjmaloney
*  dev.
*
*  Revision 1.6  2003/03/27 21:17:55  mjmaloney
*  drgs dev
*
*  Revision 1.5  2001/04/12 12:26:18  mike
*  test checkin from boss
*
*  Revision 1.4  2000/03/12 22:41:34  mike
*  Added PasswordFile & PasswordFileEntry classes.
*
*  Revision 1.3  1999/11/16 14:46:32  mike
*  Added getCString function - retrieve a null-terminated C string from a byte
*  array.
*
*  Revision 1.2  1999/10/26 12:44:27  mike
*  Fixed sign extension problems for getting integers from bytes.
*
*  Revision 1.1  1999/10/20 21:10:04  mike
*  Initial implementation
*/
package org.opendcs.odcsapi.util;

/**
* This class contains several static methods for manipulating arrays of
* bytes.
*/
public class ApiByteUtil
{

}

