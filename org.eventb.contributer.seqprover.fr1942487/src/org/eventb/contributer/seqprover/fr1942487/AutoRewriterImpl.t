/*******************************************************************************
 * Copyright (c) 2008 ETH Zurich.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eventb.contributer.seqprover.fr1942487;

import java.math.BigInteger;
import java.util.Collection;
import java.util.ArrayList;

import org.eventb.core.ast.AssociativeExpression;
import org.eventb.core.ast.AssociativePredicate;
import org.eventb.core.ast.AtomicExpression;
import org.eventb.core.ast.BinaryExpression;
import org.eventb.core.ast.BinaryPredicate;
import org.eventb.core.ast.BoolExpression;
import org.eventb.core.ast.BoundIdentDecl;
import org.eventb.core.ast.BoundIdentifier;
import org.eventb.core.ast.DefaultRewriter;
import org.eventb.core.ast.Expression;
import org.eventb.core.ast.Formula;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.Identifier;
import org.eventb.core.ast.IntegerLiteral;
import org.eventb.core.ast.LiteralPredicate;
import org.eventb.core.ast.Predicate;
import org.eventb.core.ast.QuantifiedExpression;
import org.eventb.core.ast.QuantifiedPredicate;
import org.eventb.core.ast.RelationalPredicate;
import org.eventb.core.ast.SetExtension;
import org.eventb.core.ast.SimplePredicate;
import org.eventb.core.ast.UnaryExpression;
import org.eventb.core.ast.UnaryPredicate;
import org.eventb.core.seqprover.eventbExtensions.Lib;

/**
 * Generated formula rewriter for the Event-B sequent prover.
 */
@SuppressWarnings("unused")
public class AutoRewriterImpl extends DefaultRewriter {

	protected AssociativePredicate makeAssociativePredicate(int tag, Collection<Predicate> children) {
		return ff.makeAssociativePredicate(tag, children, null);
	}

	protected RelationalPredicate makeRelationalPredicate(int tag, Expression left,
			Expression right) {
		return ff.makeRelationalPredicate(tag, left, right, null);
	}

	protected UnaryExpression makeUnaryExpression(int tag, Expression child) {
		return ff.makeUnaryExpression(tag, child, null);
	}

	public AutoRewriterImpl() {
		super(true, FormulaFactory.getDefault());
	}
		
	%include {Formula.tom}
	
	@Override
	public Predicate rewrite(RelationalPredicate predicate) {
		
		%match (Predicate predicate) {
			/**
	         * Remove membership for range: E∈a‥b = a≤E∧E≤b
	         */
			In(left, UpTo(from, to))->{
				
				ArrayList<Predicate> and_children = new ArrayList<Predicate>(2);
				and_children.add(0, makeRelationalPredicate(Formula.LE, `from, `left ));
				and_children.add(1, makeRelationalPredicate(Formula.LE, `left, `to));
				return makeAssociativePredicate(Formula.LAND, and_children);				
							
			}
		}

		return predicate;
	}
	 
}