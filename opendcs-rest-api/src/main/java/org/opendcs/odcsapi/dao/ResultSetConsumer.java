/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

package org.opendcs.odcsapi.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * An implementation of this interface must be passed to ApiDaoBase.doQueryV. It will
 * be passed the ResultSet after inserting params and executing the PreparedStatement.
 * The implementation is typically an anonymous class or a lambda.
 */
@FunctionalInterface
public interface ResultSetConsumer
{
	public abstract void accept(ResultSet rs) throws SQLException;
}
