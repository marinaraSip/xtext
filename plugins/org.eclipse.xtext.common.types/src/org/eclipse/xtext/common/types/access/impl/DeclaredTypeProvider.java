/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.access.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.xtext.common.types.AnnotationType;
import org.eclipse.xtext.common.types.DeclaredType;
import org.eclipse.xtext.common.types.EnumerationType;
import org.eclipse.xtext.common.types.Executable;
import org.eclipse.xtext.common.types.FormalParameter;
import org.eclipse.xtext.common.types.GenericType;
import org.eclipse.xtext.common.types.LowerBound;
import org.eclipse.xtext.common.types.Operation;
import org.eclipse.xtext.common.types.ReferenceTypeParameter;
import org.eclipse.xtext.common.types.TypeParameter;
import org.eclipse.xtext.common.types.TypeReference;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.common.types.UpperBound;
import org.eclipse.xtext.common.types.Wildcard;
import org.eclipse.xtext.common.types.WildcardTypeParameter;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class DeclaredTypeProvider implements IClasspathTypeProvider {

	private final ClassURIHelper uriHelper;

	public DeclaredTypeProvider(ClassURIHelper uriHelper) {
		this.uriHelper = uriHelper;
	}
	
	public <T> DeclaredType createType(Class<T> clazz) {
		if (clazz.isAnonymousClass())
			throw new IllegalStateException("Cannot create type for anonymous class");
		if (clazz.isAnnotation())
			return createAnnotationType(clazz);
		if (clazz.isEnum())
			return createEnumerationType(clazz);
		
		GenericType result = TypesFactory.eINSTANCE.createGenericType();
		result.setAbstract(Modifier.isAbstract(clazz.getModifiers()));
		result.setFinal(Modifier.isFinal(clazz.getModifiers()));
		result.setInterface(clazz.isInterface());
		result.setStatic(Modifier.isStatic(clazz.getModifiers()));
		if (Modifier.isPrivate(clazz.getModifiers()))
			result.setVisibility("private");
		else if (Modifier.isProtected(clazz.getModifiers()))
			result.setVisibility("protected");
		else if (Modifier.isPublic(clazz.getModifiers()))
			result.setVisibility("public");
		result.setFullyQualifiedName(clazz.getName());
		for(Class<?> declaredClass: clazz.getDeclaredClasses()) {
			if (!declaredClass.isAnonymousClass()) {
				result.getMembers().add(createType(declaredClass));
			}
		}
		for(Method method: clazz.getDeclaredMethods()) {
			result.getMembers().add(createOperation(method));
		}
		for(Constructor<T> constructor: clazz.getDeclaredConstructors()) {
			result.getMembers().add(createConstructor(constructor));
		}
		for(Field field: clazz.getDeclaredFields()) {
			result.getMembers().add(createField(field));
		}
		if (clazz.getGenericSuperclass() != null)
			result.getSuperTypes().add(createTypeReference(clazz.getGenericSuperclass(), result));
		for(Type type: clazz.getGenericInterfaces()) {
			result.getSuperTypes().add(createTypeReference(type, result));
		}
		for(TypeVariable<?> variable: clazz.getTypeParameters()) {
			result.getTypeVariables().add(createTypeVariable(variable, result));
		}
		return result;
	}

	public org.eclipse.xtext.common.types.TypeVariable createTypeVariable(TypeVariable<?> variable, org.eclipse.xtext.common.types.Member container) {
		org.eclipse.xtext.common.types.TypeVariable result = TypesFactory.eINSTANCE.createTypeVariable();
		result.setName(variable.getName());
		if (variable.getBounds().length != 0) {
			UpperBound upperBound = TypesFactory.eINSTANCE.createUpperBound();
			for(Type bound: variable.getBounds()) {
				upperBound.getReferencedTypes().add(createTypeReference(bound, container));
			}
			result.getConstraints().add(upperBound);
		}
		return result;
	}
	
	public TypeReference createTypeReference(Type type, org.eclipse.xtext.common.types.Member container) {
		TypeReference result = TypesFactory.eINSTANCE.createTypeReference();
		if (type instanceof ParameterizedType) {
			String name = uriHelper.computeTypeName(type);
			for(org.eclipse.xtext.common.types.ParameterizedType existingType: container.getDeclaredParameterizedTypes()) {
				if (name.equals(existingType.getCanonicalName())) {
					result.setType(existingType);
					return result;
				}
			}
			ParameterizedType parameterizedType = (ParameterizedType) type;
			org.eclipse.xtext.common.types.ParameterizedType newParameterizedType = TypesFactory.eINSTANCE.createParameterizedType();
			newParameterizedType.setRawType(createTypeReference(parameterizedType.getRawType(), container));
			for(int i=0; i < parameterizedType.getActualTypeArguments().length; i++) {
				TypeParameter parameter = createTypeParameter(parameterizedType.getActualTypeArguments()[i], container, parameterizedType.getRawType(), i);
				newParameterizedType.getParameters().add(parameter);
			}
			container.getDeclaredParameterizedTypes().add(newParameterizedType);
			result.setType(newParameterizedType);
		} else {
			result.setType(createProxy(type));
		}
		return result;
	}
	
	private TypeParameter createTypeParameter(Type actualTypeArgument,
			org.eclipse.xtext.common.types.Member container, Type rawType, int i) {
		if (actualTypeArgument instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType) actualTypeArgument;
			WildcardTypeParameter result = TypesFactory.eINSTANCE.createWildcardTypeParameter();
			Wildcard wildcard = TypesFactory.eINSTANCE.createWildcard();
			if (wildcardType.getUpperBounds().length != 0) {
				UpperBound upperBound = TypesFactory.eINSTANCE.createUpperBound();
				for(Type boundType: wildcardType.getUpperBounds()) {
					TypeReference reference = createTypeReference(boundType, container);
					upperBound.getReferencedTypes().add(reference);
				}
				wildcard.getConstraints().add(upperBound);
			}
			if (wildcardType.getLowerBounds().length != 0) {
				LowerBound lowerBound = TypesFactory.eINSTANCE.createLowerBound();
				for(Type boundType: wildcardType.getLowerBounds()) {
					TypeReference reference = createTypeReference(boundType, container);
					lowerBound.getReferencedTypes().add(reference);
				}
				wildcard.getConstraints().add(lowerBound);
			}
			result.setWildcard(wildcard);
			return result;
		} else {
			ReferenceTypeParameter result = TypesFactory.eINSTANCE.createReferenceTypeParameter();
			TypeReference typeReference = createTypeReference(actualTypeArgument, container);
			result.setReference(typeReference);
			return result;
		}
	}

	public org.eclipse.xtext.common.types.Type createProxy(Type type) {
		InternalEObject proxy = (InternalEObject) TypesFactory.eINSTANCE.createVoid();
		URI uri = uriHelper.getFullURI(type);
		proxy.eSetProxyURI(uri);
		return (org.eclipse.xtext.common.types.Type) proxy;
	}
	
	public AnnotationType createAnnotationType(Class<?> clazz) {
		throw new UnsupportedOperationException();
	}
	
	public EnumerationType createEnumerationType(Class<?> clazz) {
		throw new UnsupportedOperationException();
	}
	
	public org.eclipse.xtext.common.types.Field createField(Field field) {
		org.eclipse.xtext.common.types.Field result = TypesFactory.eINSTANCE.createField();
		result.setFullyQualifiedName(field.getDeclaringClass().getName() + "." + field.getName());
		result.setFinal(Modifier.isFinal(field.getModifiers()));
		result.setStatic(Modifier.isStatic(field.getModifiers()));
		if (Modifier.isPrivate(field.getModifiers()))
			result.setVisibility("private");
		else if (Modifier.isProtected(field.getModifiers()))
			result.setVisibility("protected");
		else if (Modifier.isPublic(field.getModifiers()))
			result.setVisibility("public");
		result.setType(createTypeReference(field.getGenericType(), result));
		return result;
	}

	public <T> org.eclipse.xtext.common.types.Constructor createConstructor(Constructor<T> constructor) {
		org.eclipse.xtext.common.types.Constructor result = TypesFactory.eINSTANCE.createConstructor();
		enhanceExecutable(result, constructor, constructor.getGenericParameterTypes());
		enhanceGenericDeclaration(result, constructor);
		for(Type parameterType : constructor.getGenericExceptionTypes()) {
			result.getExceptions().add(createTypeReference(parameterType, result));
		}
		return result;
	}

	public void enhanceExecutable(Executable result, Member member, Type[] parameterTypes) {
		StringBuilder fqName = new StringBuilder(48);
		fqName.append(member.getDeclaringClass().getName());
		fqName.append('.');
		fqName.append(member.getName());
		fqName.append('(');
		for (int i = 0; i < parameterTypes.length; i++) {
			if (i != 0)
				fqName.append(',');
			uriHelper.computeTypeName(parameterTypes[i], fqName);
		}
		fqName.append(')');
		result.setFullyQualifiedName(fqName.toString());
		if (Modifier.isPrivate(member.getModifiers()))
			result.setVisibility("private");
		else if (Modifier.isProtected(member.getModifiers()))
			result.setVisibility("protected");
		else if (Modifier.isPublic(member.getModifiers()))
			result.setVisibility("public");
		int i = 0;
		for(Type parameterType : parameterTypes) {
			result.getParameters().add(createFormalParameter(parameterType, "p" + i, result));
			i++;
		}
	}
	
	public void enhanceGenericDeclaration(Executable result, GenericDeclaration declaration) {
		for(TypeVariable<?> variable: declaration.getTypeParameters()) {
			result.getTypeVariables().add(createTypeVariable(variable, result));
		}
	}

	public Operation createOperation(Method method) {
		Operation result = TypesFactory.eINSTANCE.createOperation();
		enhanceExecutable(result, method, method.getGenericParameterTypes());
		enhanceGenericDeclaration(result, method);
		result.setFinal(Modifier.isFinal(method.getModifiers()));
		result.setStatic(Modifier.isStatic(method.getModifiers()));
		result.setReturnType(createTypeReference(method.getGenericReturnType(), result));
		for(Type parameterType : method.getGenericExceptionTypes()) {
			result.getExceptions().add(createTypeReference(parameterType, result));
		}
		return result;
	}

	public FormalParameter createFormalParameter(Type parameterType, String paramName, org.eclipse.xtext.common.types.Member container) {
		FormalParameter result = TypesFactory.eINSTANCE.createFormalParameter();
		result.setName(paramName);
		result.setParameterType(createTypeReference(parameterType, container));
		return result;
	}

}
