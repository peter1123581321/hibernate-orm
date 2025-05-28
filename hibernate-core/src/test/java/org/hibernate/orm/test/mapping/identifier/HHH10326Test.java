/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.HibernateException;
import org.hibernate.annotations.NaturalId;
import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey("HHH-10326")
@DomainModel(
		annotatedClasses = {
				HHH10326Test.BookSingleImmutable.class,
				HHH10326Test.BookSingleMutable.class,
				HHH10326Test.BookMultipleImmutable.class,
				HHH10326Test.BookMultipleMutable.class,
				HHH10326Test.BookMultipleMixed.class
		}
)
@SessionFactory
class HHH10326Test {

	@Test
	void hhh10326Test_singleImmutableNaturalId_updateFails(SessionFactoryScope scope) {

		// store entity
		scope.inTransaction( session -> {
			BookSingleImmutable book = new BookSingleImmutable();
			book.title = "title";
			book.isbn = "123456";
			session.persist( book );
		} );

		// get entity and change the natural id
		HibernateException hibernateException = assertThrows( HibernateException.class, () ->
				scope.inTransaction( session -> {
					SelectionQuery<BookSingleImmutable> query = session.createSelectionQuery(
							"select d from BookSingleImmutable d", BookSingleImmutable.class );
					BookSingleImmutable book = query.getSingleResult();
					assertEquals( "123456", book.isbn );
					assertEquals( "title", book.title );

					book.isbn = "789012";
					session.persist( book );
				} )
		);
		assertEquals(
				"An immutable natural identifier of entity org.hibernate.orm.test.mapping.identifier.HHH10326Test$BookSingleImmutable was altered from `123456` to `789012`",
				hibernateException.getMessage() );

		// assert
		scope.inTransaction( session -> {
			SelectionQuery<BookSingleImmutable> query = session.createSelectionQuery(
					"select d from BookSingleImmutable d", BookSingleImmutable.class );
			BookSingleImmutable book = query.getSingleResult();
			assertEquals( "123456", book.isbn ); // old value
		} );
	}

	@Test
	void hhh10326Test_singleMutableNaturalId_updateSuccessful(SessionFactoryScope scope) {

		// store entity
		scope.inTransaction( session -> {
			BookSingleMutable book = new BookSingleMutable();
			book.title = "title";
			book.isbn = "123456";
			session.persist( book );
		} );

		// get entity and change the natural id
		scope.inTransaction( session -> {
			SelectionQuery<BookSingleMutable> query = session.createSelectionQuery( "select d from BookSingleMutable d",
					BookSingleMutable.class );
			BookSingleMutable book = query.getSingleResult();
			assertEquals( "123456", book.isbn );
			assertEquals( "title", book.title );

			book.isbn = "789012";
			session.persist( book );
		} );

		// assert
		scope.inTransaction( session -> {
			SelectionQuery<BookSingleMutable> query = session.createSelectionQuery( "select d from BookSingleMutable d",
					BookSingleMutable.class );
			BookSingleMutable book = query.getSingleResult();
			assertEquals( "789012", book.isbn ); // new value
		} );
	}

	@Test
	void hhh10326Test_multipleImmutableNaturalId_updateFails(SessionFactoryScope scope) {

		// store entity
		scope.inTransaction( session -> {
			BookMultipleImmutable book = new BookMultipleImmutable();
			book.title = "title";
			book.isbn1 = "123456";
			book.isbn2 = "123456";
			session.persist( book );
		} );

		// get entity and change the natural id
		HibernateException hibernateException = assertThrows( HibernateException.class, () ->
				scope.inTransaction( session -> {
					SelectionQuery<BookMultipleImmutable> query = session.createSelectionQuery(
							"select d from BookMultipleImmutable d", BookMultipleImmutable.class );
					BookMultipleImmutable book = query.getSingleResult();
					assertEquals( "123456", book.isbn1 );
					assertEquals( "123456", book.isbn2 );
					assertEquals( "title", book.title );

					book.isbn1 = "789012";
					book.isbn2 = "789012";
					session.persist( book );
				} )
		);
		assertEquals(
				"An immutable attribute [isbn1] within compound natural identifier of entity org.hibernate.orm.test.mapping.identifier.HHH10326Test$BookMultipleImmutable was altered from `123456` to `789012`",
				hibernateException.getMessage() );

		// assert
		scope.inTransaction( session -> {
			SelectionQuery<BookMultipleImmutable> query = session.createSelectionQuery(
					"select d from BookMultipleImmutable d", BookMultipleImmutable.class );
			BookMultipleImmutable book = query.getSingleResult();
			assertEquals( "123456", book.isbn1 ); // old value
			assertEquals( "123456", book.isbn2 ); // old value
		} );
	}

	@Test
	void hhh10326Test_multipleMutableNaturalId_updateSuccessful(SessionFactoryScope scope) {

		// store entity
		scope.inTransaction( session -> {
			BookMultipleMutable book = new BookMultipleMutable();
			book.title = "title";
			book.isbn1 = "123456";
			book.isbn2 = "123456";
			session.persist( book );
		} );

		// get entity and change the natural id
		scope.inTransaction( session -> {
			SelectionQuery<BookMultipleMutable> query = session.createSelectionQuery(
					"select d from BookMultipleMutable d", BookMultipleMutable.class );
			BookMultipleMutable book = query.getSingleResult();
			assertEquals( "123456", book.isbn1 );
			assertEquals( "123456", book.isbn2 );
			assertEquals( "title", book.title );

			book.isbn1 = "789012";
			book.isbn2 = "789012";
			session.persist( book );
		} );

		// assert
		scope.inTransaction( session -> {
			SelectionQuery<BookMultipleMutable> query = session.createSelectionQuery(
					"select d from BookMultipleMutable d", BookMultipleMutable.class );
			BookMultipleMutable book = query.getSingleResult();
			assertEquals( "789012", book.isbn1 ); // new value
			assertEquals( "789012", book.isbn2 ); // new value
		} );
	}

	@Test
	void hhh10326Test_multipleMixedNaturalId(SessionFactoryScope scope) {

		// store entity
		scope.inTransaction( session -> {
			BookMultipleMixed book = new BookMultipleMixed();
			book.title = "title";
			book.isbn1 = "123456";
			book.isbn2 = "123456";
			session.persist( book );
		} );

		// get entity and change the natural id
		scope.inTransaction( session -> {
			SelectionQuery<BookMultipleMixed> query = session.createSelectionQuery( "select d from BookMultipleMixed d",
					BookMultipleMixed.class );
			BookMultipleMixed book = query.getSingleResult();
			assertEquals( "123456", book.isbn1 );
			assertEquals( "123456", book.isbn2 );
			assertEquals( "title", book.title );

			book.isbn1 = "789012";
			book.isbn2 = "789012";
			session.persist( book );
		} );

		// assert
		scope.inTransaction( session -> {
			SelectionQuery<BookMultipleMixed> query = session.createSelectionQuery( "select d from BookMultipleMixed d",
					BookMultipleMixed.class );
			BookMultipleMixed book = query.getSingleResult();
			assertEquals( "789012", book.isbn1 ); // new value
			assertEquals( "123456", book.isbn2 ); // old value
		} );
	}

	@Entity(name = "BookSingleImmutable")
	public static class BookSingleImmutable {

		@Id
		@GeneratedValue
		long id;

		@NaturalId
		String isbn;

		String title;
	}

	@Entity(name = "BookSingleMutable")
	public static class BookSingleMutable {

		@Id
		@GeneratedValue
		long id;

		@NaturalId(mutable = true)
		String isbn;

		String title;
	}

	@Entity(name = "BookMultipleImmutable")
	public static class BookMultipleImmutable {

		@Id
		@GeneratedValue
		long id;

		@NaturalId
		String isbn1;

		@NaturalId
		String isbn2;

		String title;
	}

	@Entity(name = "BookMultipleMutable")
	public static class BookMultipleMutable {

		@Id
		@GeneratedValue
		long id;

		@NaturalId(mutable = true)
		String isbn1;

		@NaturalId(mutable = true)
		String isbn2;

		String title;
	}

	@Entity(name = "BookMultipleMixed")
	public static class BookMultipleMixed {

		@Id
		@GeneratedValue
		long id;

		@NaturalId(mutable = true)
		String isbn1;

		@NaturalId
		String isbn2;

		String title;
	}

}
