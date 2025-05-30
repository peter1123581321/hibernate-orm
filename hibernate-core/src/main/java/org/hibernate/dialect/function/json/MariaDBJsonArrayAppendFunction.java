/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * MariaDB json_array_append function.
 */
public class MariaDBJsonArrayAppendFunction extends AbstractJsonArrayAppendFunction {

	public MariaDBJsonArrayAppendFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> translator) {
		final Expression json = (Expression) arguments.get( 0 );
		final Expression jsonPath = (Expression) arguments.get( 1 );
		final SqlAstNode value = arguments.get( 2 );
		sqlAppender.appendSql( "json_replace(" );
		json.accept( translator );
		sqlAppender.appendSql( ',' );
		jsonPath.accept( translator );
		sqlAppender.appendSql( ",json_merge(json_extract(" );
		json.accept( translator );
		sqlAppender.appendSql( ',' );
		jsonPath.accept( translator );
		sqlAppender.appendSql( "),json_array(" );
		value.accept( translator );
		sqlAppender.appendSql( ")))" );

	}
}
