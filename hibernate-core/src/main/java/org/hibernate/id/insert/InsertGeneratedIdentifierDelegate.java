/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;

/**
 * Each implementation defines a strategy for retrieving a primary key
 * {@linkplain org.hibernate.generator.OnExecutionGenerator generated by
 * the database} from the database after execution of an {@code insert}
 * statement. The generated primary key is usually an {@code IDENTITY}
 * column, but in principle it might be something else, for example,
 * a value generated by a trigger.
 * <p>
 * An implementation controls:
 * <ul>
 * <li>building the SQL {@code insert} statement, and
 * <li>retrieving the generated identifier value using JDBC.
 * </ul>
 * <p>
 * The implementation should be written to handle any instance of
 * {@link org.hibernate.generator.OnExecutionGenerator}.
 *
 * @see org.hibernate.generator.OnExecutionGenerator
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@link GeneratedValuesMutationDelegate} instead.
 */
@Deprecated(since = "6.5", forRemoval = true)
public interface InsertGeneratedIdentifierDelegate extends GeneratedValuesMutationDelegate {

	@Override
	PreparedStatement prepareStatement(String insertSql, SharedSessionContractImplementor session);

	/**
	 * Append SQL specific to this delegate's mode of handling generated
	 * primary key values to the given {@code insert} statement.
	 *
	 * @return The processed {@code insert} statement string
	 */
	default String prepareIdentifierGeneratingInsert(String insertSQL) {
		return insertSQL;
	}

	/**
	 * Execute the given {@code insert} statement and return the generated
	 * key value.
	 *
	 * @param insertSQL The {@code insert} statement string
	 * @param session The session in which we are operating
	 * @param binder The parameter binder
	 *
	 * @return The generated identifier value
	 */
	GeneratedValues performInsertReturning(String insertSQL, SharedSessionContractImplementor session, Binder binder);
}
