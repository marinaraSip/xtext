/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.parser;

import org.eclipse.xtext.parser.impl.PartialParsingPointers;
import org.eclipse.xtext.testlanguages.LookaheadTestLanguageStandaloneSetup;

/**
 * @author Jan K�hnlein - Initial contribution and API
 * @author Sebastian Zarnekow
 */
public class PartialParsingPointerLookaheadTest extends AbstractPartialParsingPointerTest {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		with(LookaheadTestLanguageStandaloneSetup.class);
	}

	public void testLookahead_0_2() throws Exception {
		String model = "bar a foo bar c b d foo bar b c";
		for (int i = 0; i < 3; ++i) {
			PartialParsingPointers parsingPointers = calculatePartialParsingPointers(model, i, 1);
			checkParseRegionPointers(parsingPointers, model, "ParserRule", "Entry");
		}
	}
	
	public void testLookahead_3_4() throws Exception {
		String model = "bar a foo bar c b d foo bar b c";
		for (int i = 3; i < 5; ++i) {
			PartialParsingPointers parsingPointers = calculatePartialParsingPointers(model, i, 1);
			checkParseRegionPointers(parsingPointers, "bar a", "RuleCall", "LookAhead0");
		}
	}
	
	public void testLookahead_6_8() throws Exception {
		String model = "bar a foo bar c b d foo bar b c";
		for (int i = 6; i < 9; ++i) {
			PartialParsingPointers parsingPointers = calculatePartialParsingPointers(model, i, 1);
			checkParseRegionPointers(parsingPointers, model, "ParserRule", "Entry");
		}
	}

	public void testLookahead_9_14() throws Exception {
		String model = "bar a foo bar c b d foo bar b c";
		for (int i = 9; i < 15; ++i) {
			PartialParsingPointers parsingPointers = calculatePartialParsingPointers(model, i, 1);
			checkParseRegionPointers(parsingPointers, " foo bar c b d", "RuleCall", "Alts");
		}
	}

	public void testLookahead_15_18() throws Exception {
		String model = "bar a foo bar c b d foo bar b c";
		for (int i = 15; i < 19; ++i) {
			PartialParsingPointers parsingPointers = calculatePartialParsingPointers(model, i, 1);
			checkParseRegionPointers(parsingPointers, " foo bar c b d", "RuleCall", "LookAhead1");
		}
	}
	
	public void testLookahead_19_22() throws Exception {
		String model = "bar a foo bar c b d foo bar b c";
		for (int i = 19; i < 22; ++i) {
			PartialParsingPointers parsingPointers = calculatePartialParsingPointers(model, i, 1);
			checkParseRegionPointers(parsingPointers, model, "ParserRule", "Entry");
		}
	}

	public void testLookahead_23_28() throws Exception {
		String model = "bar a foo bar c b d foo bar b c";
		for (int i = 23; i < 29; ++i) {
			PartialParsingPointers parsingPointers = calculatePartialParsingPointers(model, i, 1);
			checkParseRegionPointers(parsingPointers, " foo bar b c", "RuleCall", "Alts");
		}
	}
	
	public void testLookahead_29() throws Exception {
		String model = "bar a foo bar c b d foo bar b c";
		for (int i = 29; i < model.length(); ++i) {
			PartialParsingPointers parsingPointers = calculatePartialParsingPointers(model, i, 1);
			checkParseRegionPointers(parsingPointers, " c", "RuleCall", "LookAhead4");
		}
	}
	
}
