/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;

import jakarta.persistence.metamodel.EntityType;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmManagedDomainType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPersistentAttribute;
import org.hibernate.query.sqm.tree.domain.SqmEntityDomainType;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Bindable.BindableType.ENTITY_TYPE;
import static jakarta.persistence.metamodel.Type.PersistenceType.ENTITY;
import static org.hibernate.metamodel.model.domain.internal.DomainModelHelper.isCompatible;

/**
 * Implementation of {@link EntityType}.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class EntityTypeImpl<J>
		extends AbstractIdentifiableType<J>
		implements SqmEntityDomainType<J>, Serializable {

	private final String jpaEntityName;
	private final JpaMetamodelImplementor metamodel;
	private final SqmPathSource<?> discriminatorPathSource;

	public EntityTypeImpl(
			String entityName,
			String jpaEntityName,
			boolean hasIdClass,
			boolean hasIdProperty,
			boolean hasVersion,
			JavaType<J> javaType,
			IdentifiableDomainType<? super J> superType,
			JpaMetamodelImplementor metamodel) {
		super(
				entityName,
				javaType,
				superType,
				hasIdClass,
				hasIdProperty,
				hasVersion,
				metamodel
		);

		this.jpaEntityName = jpaEntityName;
		this.metamodel = metamodel;

		discriminatorPathSource = entityDiscriminatorPathSource( metamodel );
	}

	private EntityDiscriminatorSqmPathSource<?> entityDiscriminatorPathSource(JpaMetamodelImplementor metamodel) {
		final EntityPersister entityDescriptor =
				metamodel.getMappingMetamodel()
						.getEntityDescriptor( getHibernateEntityName() );
		final DiscriminatorType<?> discriminatorType = entityDescriptor.getDiscriminatorDomainType();
		return discriminatorType == null ? null
				: new EntityDiscriminatorSqmPathSource<>( discriminatorType, this, entityDescriptor );
	}

	public EntityTypeImpl(
			JavaType<J> javaType,
			IdentifiableDomainType<? super J> superType,
			PersistentClass persistentClass,
			JpaMetamodelImplementor metamodel) {
		this(
				persistentClass.getEntityName(),
				persistentClass.getJpaEntityName(),
				persistentClass.getDeclaredIdentifierMapper() != null
						|| superType != null && superType.hasIdClass(),
				persistentClass.hasIdentifierProperty(),
				persistentClass.isVersioned(),
				javaType,
				superType,
				metamodel
		);
	}

	public EntityTypeImpl(JavaType<J> javaTypeDescriptor, JpaMetamodelImplementor metamodel) {
		super(
				javaTypeDescriptor.getJavaTypeClass().getName(),
				javaTypeDescriptor,
				null,
				false,
				false,
				false,
				metamodel
		);

		this.jpaEntityName = javaTypeDescriptor.getJavaTypeClass().getName();
		this.metamodel = metamodel;
		this.discriminatorPathSource = null;
	}

	@Override
	public String getName() {
		return jpaEntityName;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getJavaType();
	}

	@Override
	public String getHibernateEntityName() {
		return super.getTypeName();
	}

	@Override
	public SqmEntityDomainType<J> getSqmType() {
		return this;
	}

	@Override
	public String getPathName() {
		return getHibernateEntityName();
	}

	@Override
	public SqmEntityDomainType<J> getPathType() {
		return this;
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		final PersistentAttribute<? super J,?> attribute = super.findAttribute( name );
		if ( attribute != null ) {
			return (SqmPathSource<?>) attribute;
		}
		else if ( EntityIdentifierMapping.matchesRoleName( name ) ) {
			return hasSingleIdAttribute() ? findIdAttribute() : getIdentifierDescriptor();
		}
		else if ( EntityVersionMapping.matchesRoleName( name ) ) {
			return hasVersionAttribute() ? findVersionAttribute() : null;
		}
		else if ( EntityDiscriminatorMapping.matchesRoleName( name ) ) {
			return discriminatorPathSource;
		}
		else {
			return null;
		}
	}

	@Override
	public SqmPathSource<?> getIdentifierDescriptor() {
		return super.getIdentifierDescriptor();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
		final PersistentAttribute<? super J,?> attribute = super.findAttribute( name );
		if ( attribute != null ) {
			return (SqmPathSource<?>) attribute;
		}
		else {
			if ( includeSubtypes ) {
				final PersistentAttribute<?, ?> subtypeAttribute = findSubtypeAttribute( name );
				if ( subtypeAttribute != null ) {
					return (SqmPathSource<?>) subtypeAttribute;
				}
			}
			if ( EntityIdentifierMapping.matchesRoleName( name ) ) {
				return hasSingleIdAttribute() ? findIdAttribute() : getIdentifierDescriptor();
			}
			else if ( EntityDiscriminatorMapping.matchesRoleName( name ) ) {
				return discriminatorPathSource;
			}
			else {
				return null;
			}
		}
	}

	private SqmPersistentAttribute<?, ?> findSubtypeAttribute(String name) {
		SqmPersistentAttribute<?,?> subtypeAttribute = null;
		for ( SqmManagedDomainType<?> subtype : getSubTypes() ) {
			final SqmPersistentAttribute<?,?> candidate = subtype.findSubTypesAttribute( name );
			if ( candidate != null ) {
				if ( subtypeAttribute != null
						&& !isCompatible( subtypeAttribute, candidate, metamodel.getMappingMetamodel() ) ) {
					throw new PathException(
							String.format(
									Locale.ROOT,
									"Could not resolve attribute '%s' of '%s' due to the attribute being declared in multiple subtypes '%s' and '%s'",
									name,
									getTypeName(),
									subtypeAttribute.getDeclaringType().getTypeName(),
									candidate.getDeclaringType().getTypeName()
							)
					);
				}
				subtypeAttribute = candidate;
			}
		}
		return subtypeAttribute;
	}

	@Override
	public SqmPersistentAttribute<? super J, ?> findAttribute(String name) {
		final var attribute = super.findAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}
		else if ( EntityIdentifierMapping.matchesRoleName( name ) ) {
			return findIdAttribute();
		}
		else {
			return null;
		}
	}

	@Override
	public BindableType getBindableType() {
		return ENTITY_TYPE;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return ENTITY;
	}

	@Override
	public Collection<? extends SqmEntityDomainType<? extends J>> getSubTypes() {
		//noinspection unchecked
		return (Collection<? extends SqmEntityDomainType<? extends J>>) super.getSubTypes();
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		throw new UnsupportedMappingException(
				"EntityType cannot be used to create an SqmPath - that would be an SqmFrom which are created directly"
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization

	@Serial
	protected Object writeReplace() throws ObjectStreamException {
		return new SerialForm( metamodel, getHibernateEntityName() );
	}

	private static class SerialForm implements Serializable {
		private final JpaMetamodel jpaMetamodel;
		private final String hibernateEntityName;

		public SerialForm(JpaMetamodel jpaMetamodel, String hibernateEntityName) {
			this.jpaMetamodel = jpaMetamodel;
			this.hibernateEntityName = hibernateEntityName;
		}

		@Serial
		private Object readResolve() {
			return jpaMetamodel.entity( hibernateEntityName );
		}

	}
}
