/*
 * generated by Xtext
 */
package org.eclipse.xtext.common.types.xtext.ui;

import org.eclipse.xtext.common.types.xtext.JvmIdentifiableQualifiedNameProvider;
import org.eclipse.xtext.naming.IQualifiedNameProvider;

/**
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 */
public class RefactoringTestLanguageRuntimeModule extends org.eclipse.xtext.common.types.xtext.ui.AbstractRefactoringTestLanguageRuntimeModule {

	@Override
	public Class<? extends IQualifiedNameProvider> bindIQualifiedNameProvider() {
		return JvmIdentifiableQualifiedNameProvider.class;
	}
}
