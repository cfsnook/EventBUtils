package ch.ethz.eventb.internal.decomposition.wizards;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eventb.core.IAction;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.IInvariant;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IParameter;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.IVariable;
import org.eventb.core.IConvergenceElement.Convergence;
import org.eventb.core.ast.Assignment;
import org.eventb.core.ast.BecomesSuchThat;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.Predicate;
import org.eventb.core.ast.SourceLocation;
import org.eventb.core.seqprover.eventbExtensions.Lib;
import org.rodinp.core.RodinDBException;

public class DecompositionUtils {

	enum DecomposedEventType {
		EXTERNAL(1), INTERNAL(2), NONE(0);

		private final int code;

		DecomposedEventType(int code) {
			this.code = code;
		}

		public int getCode() {
			return code;
		}
	}

	/**
	 * Utility method for creating variables in an input machine given an
	 * element distribution. This is done by copying the accessed variables of
	 * the distribution to the machine as either shared variables (
	 * {@link #createSharedVariable(IMachineRoot, String)}) or as private
	 * variables ({@link #createPrivateVariable(IMachineRoot, String)}).
	 * 
	 * @param mch
	 *            a machine.
	 * @param dist
	 *            an element distribution.
	 * @param monitor
	 *            a progress monitor.
	 * @throws RodinDBException
	 *             if some errors occurred when
	 *             <ul>
	 *             <li>getting the shared variables of the input model
	 *             distribution {@link IModelDistribution#getSharedVariables()}.
	 *             </li>
	 *             <li>creating the shared variables
	 *             {@link #createSharedVariable(IMachineRoot, String)}.</li>
	 *             <li>creating the private variables
	 *             {@link #createPrivateVariable(IMachineRoot, String)}.</li>
	 *             </ul>
	 */
	public static void createVariables(IMachineRoot mch,
			IElementDistribution dist, IProgressMonitor monitor)
			throws RodinDBException {
		Set<String> vars = dist.getAccessedVariables();
		IModelDistribution modelDist = dist.getModelDistribution();

		Collection<String> sharedVars = modelDist.getSharedVariables();
		monitor.beginTask("Create variables", vars.size());
		for (String var : vars) {
			monitor.subTask("Create variable " + var);
			if (sharedVars.contains(var)) {
				createSharedVariable(mch, var);
			} else {
				createPrivateVariable(mch, var);
			}
			monitor.worked(1);
		}
		monitor.done();
	}

	/**
	 * Utility method for creating a shared variable in a machine with a given
	 * identifier.
	 * 
	 * @param mch
	 *            a machine
	 * @param ident
	 *            the identifier of the variable
	 * @return the newly created shared variable.
	 * @throws RodinDBException
	 *             if some errors occurred when
	 *             <ul>
	 *             <li>creating the new variable in the input machine
	 *             {@link IMachineRoot#createChild(org.rodinp.core.IInternalElementType, org.rodinp.core.IInternalElement, IProgressMonitor)}
	 *             .</li>
	 *             <li>setting the identifier string of the created variable
	 *             {@link IVariable#setIdentifierString(String, IProgressMonitor)}
	 *             .</li>
	 *             <li>setting the comment of the created variable
	 *             {@link IVariable#setComment(String, IProgressMonitor)}.</li>
	 *             </ul>
	 *             TODO: Changed when the shared/private attribute is defined.
	 */
	private static IVariable createSharedVariable(IMachineRoot mch, String ident)
			throws RodinDBException {
		IVariable var = mch.createChild(IVariable.ELEMENT_TYPE, null,
				new NullProgressMonitor());
		var.setIdentifierString(ident, new NullProgressMonitor());
		var.setComment("Shared variable, DO NOT REFINE",
				new NullProgressMonitor());
		return var;
	}

	/**
	 * Utility method for creating a private variable in a machine with a given
	 * identifier.
	 * 
	 * @param mch
	 *            a machine
	 * @param ident
	 *            the identifier of the variable
	 * @return the newly created private variable.
	 * @throws RodinDBException
	 *             if some errors occurred when
	 *             <ul>
	 *             <li>creating the new variable in the input machine
	 *             {@link IMachineRoot#createChild(org.rodinp.core.IInternalElementType, org.rodinp.core.IInternalElement, IProgressMonitor)}
	 *             .</li>
	 *             <li>setting the identifier string of the created variable
	 *             {@link IVariable#setIdentifierString(String, IProgressMonitor)}
	 *             .</li>
	 *             <li>setting the comment of the created variable
	 *             {@link IVariable#setComment(String, IProgressMonitor)}.</li>
	 *             </ul>
	 *             TODO: Changed when the shared/private attribute is defined.
	 */
	private static IVariable createPrivateVariable(IMachineRoot mch,
			String ident) throws RodinDBException {
		IVariable var = mch.createChild(IVariable.ELEMENT_TYPE, null,
				new NullProgressMonitor());
		var.setIdentifierString(ident, new NullProgressMonitor());
		var.setComment("Private variable", new NullProgressMonitor());
		return var;
	}

	/**
	 * Utility method for creating invariants in an input machine given an
	 * element distribution. This is done by first creating the typing theorems
	 * for the accessed variables then copying the "relevant" invariants from
	 * the source model (recursively).
	 * 
	 * @param mch
	 *            a machine.
	 * @param dist
	 *            an element distribution.
	 * @param monitor
	 *            a progress monitor.
	 * @throws RodinDBException
	 *             if some errors occurred when
	 *             <ul>
	 *             <li>creating the typing theorems for the accessed variables
	 *             of the input element distribution
	 *             {@link #createTypingTheorems(IMachineRoot, IMachineRoot, Set)}
	 *             .</li>
	 *             <li>copy invariants from the source machine associated with
	 *             the input element distribution to the input machine
	 *             {@link #copyInvariants(IMachineRoot, IMachineRoot, Set)}.</li>
	 *             </ul>
	 */
	public static void createInvariants(IMachineRoot mch,
			IElementDistribution dist, IProgressMonitor monitor)
			throws RodinDBException {
		IMachineRoot src = dist.getMachineRoot();
		Set<String> vars = dist.getAccessedVariables();
		monitor.beginTask("Create invariants", 1);

		// Create the typing theorems.
		createTypingTheorems(mch, src, vars);

		// Copy relevant invariants.
		copyInvariants(mch, src, vars);

		monitor.worked(1);
		monitor.done();
	}

	/**
	 * Utility method for creating typing theorems in an input machine, given
	 * the set of variables and the source machine contains these variables.
	 * 
	 * @param mch
	 *            a machine
	 * @param src
	 *            the source machine contains the variables.
	 * @param vars
	 *            the set of variables (in {@link String}).
	 * @throws RodinDBException
	 *             if some errors occurred when creating tying theorem for any
	 *             input variable
	 *             {@link #createTypingTheorem(IMachineRoot, IMachineRoot, String)}
	 *             .</li>
	 */
	private static void createTypingTheorems(IMachineRoot mch,
			IMachineRoot src, Set<String> vars) throws RodinDBException {
		for (String var : vars) {
			IInvariant newInv = mch.createChild(IInvariant.ELEMENT_TYPE,
					null, new NullProgressMonitor());
			newInv.setLabel("typing_" + var, new NullProgressMonitor());
			newInv.setTheorem(true, new NullProgressMonitor());
			newInv.setPredicateString(EventBUtils.getTypingTheorem(src, var),
					new NullProgressMonitor());
		}
	}

	/**
	 * Utility method for copying the relevant invariants to a set of variables
	 * from a source machine to a destination machine (recursively).
	 * 
	 * @param mch
	 *            the destination machine.
	 * @param src
	 *            the source machine.
	 * @param vars
	 *            the set of variables (in {@link String}).
	 * @throws RodinDBException
	 *             if some errors occurred when
	 *             <ul>
	 *             <li>getting REFINES clauses of the source machine
	 *             {@link IMachineRoot#getRefinesClauses()}.</li>
	 *             <li>getting the abstract machine of any REFINES clause
	 *             {@link IRefinesMachine#getAbstractMachine()}.</li>
	 *             <li>getting the invariants of the source machine
	 *             {@link IMachineRoot#getInvariants()}.</li>
	 *             <li>checking the relevant of the invariant against the input
	 *             set of variables {@link #isRelevant(IInvariant, Set)}.</li>
	 *             <li>creating the new invariant in the destination machine
	 *             {@link IMachineRoot#createChild(org.rodinp.core.IInternalElementType, org.rodinp.core.IInternalElement, IProgressMonitor)}
	 *             .</li>
	 *             <li>setting the label of the created invariant
	 *             {@link IInvariant#setLabel(String, IProgressMonitor)}.</li>
	 *             <li>setting the predicate string of the created invariant
	 *             {@link IInvariant#setPredicateString(String, IProgressMonitor)}
	 *             .</li>
	 *             <li>setting the theorem attributed of the created invariant
	 *             {@link IInvariant#setTheorem(boolean, IProgressMonitor)}.</li>
	 *             </ul>
	 */
	private static void copyInvariants(IMachineRoot mch, IMachineRoot src,
			Set<String> vars) throws RodinDBException {
		// Recursively copy from the abstract machine.
		IRefinesMachine[] refinesClauses = src.getRefinesClauses();
		if (refinesClauses.length != 0) {
			copyInvariants(mch, (IMachineRoot) refinesClauses[0]
					.getAbstractMachine().getRoot(), vars);
		}

		// Check local invariants
		IInvariant[] invs = src.getInvariants();
		for (IInvariant inv : invs) {
			if (isRelevant(inv, vars)) {
				IInvariant newInv = mch.createChild(IInvariant.ELEMENT_TYPE,
						null, new NullProgressMonitor());
				newInv.setLabel(inv.getLabel(), new NullProgressMonitor());
				newInv.setPredicateString(inv.getPredicateString(),
						new NullProgressMonitor());
				newInv.setTheorem(inv.isTheorem(), new NullProgressMonitor());
			}
		}

	}

	/**
	 * Utility method for checking if an invariant is relevant or to a set of
	 * variables or not.
	 * 
	 * @param inv
	 *            an invariant
	 * @param vars
	 *            a set of variables (in {@link String}).
	 * @return return <code>true</code> if the invariant is relevant, otherwise
	 *         return <code>false</code>.
	 * @throws RodinDBException
	 *             if some errors occurred when
	 *             <ul>
	 *             <li>getting the free identifiers of the input invariant
	 *             {@link EventBUtils#getFreeIdentifiers(IInvariant)}.</li>
	 *             <li>getting the seen carrier sets and constants of the
	 *             machine contains the input invariant
	 *             {@link EventBUtils#getSeenCarrierSetsAndConstants(IMachineRoot)}
	 *             .</li>
	 *             </ul>
	 */
	private static boolean isRelevant(IInvariant inv, Set<String> vars)
			throws RodinDBException {
		Collection<String> idents = EventBUtils.getFreeIdentifiers(inv);

		// Remove the seen carrier sets and constants.
		IMachineRoot mch = (IMachineRoot) inv.getRoot();
		idents.removeAll(EventBUtils.getSeenCarrierSetsAndConstants(mch));
		return vars.containsAll(idents);
	}

	public static void createEvents(IMachineRoot dest,
			IElementDistribution dist, IProgressMonitor monitor)
			throws RodinDBException {
		IMachineRoot src = dist.getMachineRoot();

		createInitialisation(dest, dist, monitor);

		// Create other events.
		IEvent[] evts = src.getEvents();
		for (IEvent evt : evts) {
			if (evt.isInitialisation())
				continue;
			DecomposedEventType type = getEventType(dist, evt);
			if (type == DecomposedEventType.EXTERNAL) {
				createExternalEvent(dest, dist, evt);
			} else if (type == DecomposedEventType.INTERNAL) {
				createInternalEvent(dest, dist, evt);
			}
		}
	}

	private static void createInitialisation(IMachineRoot dest,
			IElementDistribution dist, IProgressMonitor monitor)
			throws RodinDBException {
		IMachineRoot src = dist.getMachineRoot();

		// Create the new INITIALISATION.
		IEvent newInit = dest.createChild(IEvent.ELEMENT_TYPE, null,
				new NullProgressMonitor());
		newInit.setLabel(IEvent.INITIALISATION, new NullProgressMonitor());
		newInit.setConvergence(Convergence.ORDINARY, new NullProgressMonitor());
		newInit.setExtended(false, new NullProgressMonitor());

		// Get the source INITIALISATION and flatten it.
		IEvent init = EventBUtils.getInitialisation(src);
		EventBUtils.flatten(init);
		assert init != null;
		IAction[] acts = init.getActions();
		Set<String> vars = dist.getAccessedVariables();

		// Decomposing the actions.
		for (IAction act : acts) {
			// Decompose the action according to the accessed variables.
			String newAssignmentStr = decomposeAction(act, vars);

			// If there is no decomposed action, then skip it.
			if (newAssignmentStr == null)
				continue;

			// Otherwise, create the new action in the new INITIALISATION.
			IAction newAct = newInit.createChild(IAction.ELEMENT_TYPE, null,
					new NullProgressMonitor());

			newAct.setLabel(act.getLabel(), new NullProgressMonitor());
			newAct.setAssignmentString(newAssignmentStr,
					new NullProgressMonitor());
		}
	}

	/**
	 * Utility method for decomposing an action according to a set of given
	 * accessed variables.
	 * 
	 * @param act
	 *            an action.
	 * @param vars
	 *            a set of variables (in {@link String}.
	 * @return the resulting decomposed action.
	 * @throws RodinDBException
	 *             if some errors occurred when
	 *             <ul>
	 *             <li>normalising the action {@link #normalise(IAction)}.</li>
	 *             <li>getting the assignment string of the action
	 *             {@link IAction#getAssignmentString()}.</li>
	 *             <li>setting the assignment string of the action
	 *             {@link IAction#setAssignmentString(String, IProgressMonitor)}.</li>
	 *             </ul>
	 */
	public static String decomposeAction(IAction act, Set<String> vars)
			throws RodinDBException {
		// Normalise the action first.
		EventBUtils.normalise(act);
		
		// Parsing the assignment string and getting assigned variables.
		String assignmentStr = act.getAssignmentString();
		Assignment parseAssignment = Lib.parseAssignment(assignmentStr);
		FreeIdentifier[] idents = parseAssignment.getAssignedIdentifiers();

		// Getting the set of assigned variables which are also accessed
		// variables (v) and the set of assigned variables which are not
		// accessed variables (w).
		List<FreeIdentifier> v = new ArrayList<FreeIdentifier>();
		List<FreeIdentifier> w = new ArrayList<FreeIdentifier>();
		for (FreeIdentifier ident : idents) {
			if (vars.contains(ident.getName())) {
				v.add(ident);
			} else {
				w.add(ident);
			}
		}
		
		// Return nothing if it does not modify any accessed variables.
		if (v.isEmpty()) {
			return null;
		}
		
		// Do nothing if all assigned variables are accessed variables.
		if (w.isEmpty()) {
			return act.getAssignmentString();
		}
		
		// v, w :| P(v',w') ==> v :| #w'.P(v',w')
		assert parseAssignment instanceof BecomesSuchThat;
		BecomesSuchThat bcmsuch = (BecomesSuchThat) parseAssignment;
		Predicate P = bcmsuch.getCondition();
		String vList = EventBUtils.identsToCSVString(assignmentStr, v
				.toArray(new FreeIdentifier[v.size()]));
		String wPrimedList = EventBUtils.identsToPrimedCSVString(assignmentStr, w
				.toArray(new FreeIdentifier[w.size()]));

		SourceLocation srcLoc = P.getSourceLocation();
		String strP = assignmentStr.substring(srcLoc.getStart(), srcLoc
				.getEnd() + 1);
		String newAssignmentStr = vList + " :∣ ∃" + wPrimedList + "·" + strP;
		return newAssignmentStr;
	}


	private static DecomposedEventType getEventType(IElementDistribution dist,
			IEvent evt) throws RodinDBException {
		String[] evtLabels = dist.getEventLabels();
		for (String evtLabel : evtLabels) {
			if (evt.getLabel().equals(evtLabel))
				return DecomposedEventType.INTERNAL;
		}

		Collection<String> vars = dist.getAccessedVariables();
		Collection<String> idents = EventBUtils.getFreeIdentifiers(evt);
		for (String var : vars) {
			if (idents.contains(var)) {
				return DecomposedEventType.EXTERNAL;
			}
		}

		return DecomposedEventType.NONE;
	}

	private static void createExternalEvent(IMachineRoot mch,
			IElementDistribution dist, IEvent evt) throws RodinDBException {
		// Flatten the original event.
		evt = EventBUtils.flatten(evt);
		evt = EventBUtils.normalise(evt);

		// Create the new event.
		IEvent newEvt = mch.createChild(IEvent.ELEMENT_TYPE, null,
				new NullProgressMonitor());

		// Set event signature.
		newEvt.setLabel(evt.getLabel(), new NullProgressMonitor());
		newEvt.setConvergence(Convergence.ORDINARY, new NullProgressMonitor());
		newEvt.setExtended(false, new NullProgressMonitor());
		newEvt.setComment("External event, DO NOT REFINE",
				new NullProgressMonitor());

		Set<String> vars = dist.getAccessedVariables();

		// Decomposing parameters.
		decomposeParameters(newEvt, evt, vars);
		
		// Decomposing guards.
		decomposeGuards(newEvt, evt, vars);
		
		// Decomposing actions.
		decomposingActions(newEvt, evt, vars);
	}

	private static void decomposeParameters(IEvent dest, IEvent src,
			Set<String> vars) throws RodinDBException {
		// Copy parameters from the source event.
		IParameter[] params = src.getParameters();
		for (IParameter param : params) {
			IParameter newParam = dest.createChild(IParameter.ELEMENT_TYPE,
					null, new NullProgressMonitor());
			newParam.setIdentifierString(param.getIdentifierString(),
					new NullProgressMonitor());
		}
		
		// Getting the event's accessed variables that are not accessed by the
		// distribution.
		List<String> accessedVars = EventBUtils.getAccessedVars(src);
		
		List<String> wList = new ArrayList<String>();
		for (String assignedVar : accessedVars) {
			if (!vars.contains(assignedVar)) {
				wList.add(assignedVar);
			}
		}
		
		// Create the parameters with the same identifiers as w.
		for (String w : wList) {
			IParameter newParam = dest.createChild(IParameter.ELEMENT_TYPE,
					null, new NullProgressMonitor());
			newParam.setIdentifierString(w, new NullProgressMonitor());
		}
	}

	private static void decomposeGuards(IEvent dest, IEvent src,
			Set<String> accessedVars) throws RodinDBException {
		// Getting the assigned variables that are not accessed (w).
		List<String> assignedVars = EventBUtils.getAccessedVars(src);
		
		List<String> wList = new ArrayList<String>();
		for (String assignedVar : assignedVars) {
			if (!accessedVars.contains(assignedVar)) {
				wList.add(assignedVar);
			}
		}
		
		// Create the typing theorems for w.
		for (String w : wList) {
			String predicateStr = EventBUtils.getTypingTheorem(
					(IMachineRoot) src.getRoot(), w);
			IGuard newGrd = dest.createChild(IGuard.ELEMENT_TYPE, null,
					new NullProgressMonitor());
			newGrd.setLabel("typing_" + w, new NullProgressMonitor());
			newGrd.setPredicateString(predicateStr, new NullProgressMonitor());
			newGrd.setTheorem(true, new NullProgressMonitor());
		}
		
		
		// Copy guards from the source event.
		IGuard[] grds = src.getGuards();
		for (IGuard grd : grds) {
			IGuard newGrd = dest.createChild(IGuard.ELEMENT_TYPE, null,
					new NullProgressMonitor());
			newGrd.setLabel(grd.getLabel(), new NullProgressMonitor());
			newGrd.setPredicateString(grd.getPredicateString(),
					new NullProgressMonitor());
			newGrd.setTheorem(grd.isTheorem(), new NullProgressMonitor());
		}
	}

	private static void decomposingActions(IEvent dest, IEvent src,
			Set<String> vars) throws RodinDBException {
		IAction[] acts = src.getActions();
		for (IAction act : acts) {
			String newAssignmentStr = decomposeAction(act, vars);
			if (newAssignmentStr == null)
				continue;
			IAction newAct = dest.createChild(IAction.ELEMENT_TYPE, null,
					new NullProgressMonitor());
			newAct.setLabel(act.getLabel(), new NullProgressMonitor());
			newAct.setAssignmentString(newAssignmentStr,
					new NullProgressMonitor());
		}
	}

	private static void createInternalEvent(IMachineRoot mch,
			IElementDistribution dist, IEvent evt) throws RodinDBException {
		// Flatten the original event.
		evt = EventBUtils.flatten(evt);
		
		// Create the new event.
		IEvent newEvt = mch.createChild(IEvent.ELEMENT_TYPE, null,
				new NullProgressMonitor());
		
		// Set event signature.
		newEvt.setLabel(evt.getLabel(), new NullProgressMonitor());
		newEvt.setConvergence(Convergence.ORDINARY, new NullProgressMonitor());
		newEvt.setExtended(false, new NullProgressMonitor());
		
		// Copy the parameters.
		IParameter[] params = evt.getParameters();
		for (IParameter param : params) {
			IParameter newParam = newEvt.createChild(IParameter.ELEMENT_TYPE,
					null, new NullProgressMonitor());
			newParam.setIdentifierString(param.getIdentifierString(),
					new NullProgressMonitor());
		}
		
		// Copy the guards.
		IGuard[] grds = evt.getGuards();
		for (IGuard grd : grds) {
			IGuard newGrd = newEvt.createChild(IGuard.ELEMENT_TYPE, null,
					new NullProgressMonitor());
			newGrd.setLabel(grd.getLabel(), new NullProgressMonitor());
			newGrd.setPredicateString(grd.getPredicateString(),
					new NullProgressMonitor());
			newGrd.setTheorem(grd.isTheorem(), new NullProgressMonitor());
		}
		
		// Copy the actions.
		IAction[] acts = evt.getActions();
		for (IAction act : acts) {
			IAction newAct = newEvt.createChild(IAction.ELEMENT_TYPE, null,
					new NullProgressMonitor());
			newAct.setLabel(act.getLabel(), new NullProgressMonitor());
			newAct.setAssignmentString(act.getAssignmentString(),
					new NullProgressMonitor());
		}
	}
	
	
}
