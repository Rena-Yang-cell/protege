package org.protege.editor.owl.ui.renderer;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.ui.OWLDescriptionComparator;
import org.semanticweb.owl.model.*;
import org.semanticweb.owl.util.OWLDescriptionVisitorAdapter;
import org.semanticweb.owl.util.OWLObjectVisitorAdapter;
import org.semanticweb.owl.vocab.OWLRestrictedDataRangeFacetVocabulary;
import org.semanticweb.owl.vocab.XSDVocabulary;

import java.net.URI;
import java.util.*;
/*
 * Copyright (C) 2007, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: Apr 2, 2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 * <p/>
 * A renderer that renders objects using the Manchester OWL Syntax.
 */
public class OWLObjectRendererImpl extends OWLObjectVisitorAdapter implements OWLObjectRenderer {

    private static final Logger logger = Logger.getLogger(OWLObjectRendererImpl.class);

    private OWLModelManager owlModelManager;

    private StringBuilder buffer;

    private OWLEntityRenderer entityRenderer;

    private BracketWriter bracketWriter;

    private Map<OWLRestrictedDataRangeFacetVocabulary, String> facetMap;

    private Map<URI, Boolean> simpleRenderDatatypes;


    public OWLObjectRendererImpl(OWLModelManager owlModelManager) {
        this.owlModelManager = owlModelManager;
        buffer = new StringBuilder();
        bracketWriter = new BracketWriter();
        facetMap = new HashMap<OWLRestrictedDataRangeFacetVocabulary, String>();
        facetMap.put(OWLRestrictedDataRangeFacetVocabulary.MIN_EXCLUSIVE, ">");
        facetMap.put(OWLRestrictedDataRangeFacetVocabulary.MAX_EXCLUSIVE, "<");
        facetMap.put(OWLRestrictedDataRangeFacetVocabulary.MIN_INCLUSIVE, ">=");
        facetMap.put(OWLRestrictedDataRangeFacetVocabulary.MAX_INCLUSIVE, "<=");
        simpleRenderDatatypes = new HashMap<URI, Boolean>();
        simpleRenderDatatypes.put(XSDVocabulary.INT.getURI(), false);
        simpleRenderDatatypes.put(XSDVocabulary.FLOAT.getURI(), false);
        simpleRenderDatatypes.put(XSDVocabulary.DOUBLE.getURI(), false);
        simpleRenderDatatypes.put(XSDVocabulary.STRING.getURI(), true);
        simpleRenderDatatypes.put(XSDVocabulary.BOOLEAN.getURI(), false);
    }


    public void setup(OWLModelManager owlModelManager) {
    }


    public void initialise() {
    }


    public void dispose() {
    }


    protected String getAndKeyWord() {
        return "and";
    }


    protected String getOrKeyWord() {
        return "or";
    }


    protected String getNotKeyWord() {
        return "not";
    }


    protected String getSomeKeyWord() {
        return "some";
    }


    protected String getAllKeyWord() {
        return "only";
    }


    protected String getValueKeyWord() {
        return "value";
    }


    protected String getMinKeyWord() {
        return "min";
    }


    protected String getMaxKeyWord() {
        return "max";
    }


    protected String getExactlyKeyWord() {
        return "exactly";
    }


    public String render(OWLObject object, OWLEntityRenderer entityRenderer) {
        reset();
        this.entityRenderer = entityRenderer;
        try {
            object.accept(this);
            return buffer.toString();
        }
        catch (Exception e) {
            return "<Error! " + e.getMessage() + ">";
        }
    }


    protected String getRendering(OWLEntity entity) {
        owlModelManager.getRendering(entity);

        return entityRenderer.render(entity);
    }


    int lastNewLineIndex = 0;

    int currentIndex = 0;


    protected void write(String s) {
        int index = s.indexOf('\n');
        if (index != -1) {
            lastNewLineIndex = currentIndex + index;
        }
        currentIndex = currentIndex + s.length();
        buffer.append(s);
    }


    protected int getIndent() {
        return currentIndex - lastNewLineIndex;
    }


    protected void insertIndent(int indent) {
        for (int i = 0; i < indent; i++) {
            write(" ");
        }
    }


    protected void writeAndKeyword() {
        write(getAndKeyWord());
        write(" ");
    }


    public void reset() {
        lastNewLineIndex = 0;
        currentIndex = 0;
        buffer = new StringBuilder();
    }


    public String getText() {
        return buffer.toString();
    }


    private OWLDescriptionComparator comparator = new OWLDescriptionComparator(owlModelManager);


    private List<OWLDescription> sort(Set<OWLDescription> descriptions) {
        List<OWLDescription> sortedDescs = new ArrayList<OWLDescription>(descriptions);
        Collections.sort(sortedDescs, comparator);
        return sortedDescs;
    }


    public void visit(OWLObjectIntersectionOf node) {

        int indent = getIndent();
        List<OWLDescription> ops = sort(node.getOperands());
        for (int i = 0; i < ops.size(); i++) {
            OWLDescription curOp = ops.get(i);
            curOp.accept(this);
            if (i < ops.size() - 1) {
                write("\n");
                insertIndent(indent);
                if (curOp instanceof OWLClass && ops.get(i + 1) instanceof OWLRestriction) {
                    write("that ");
                }
                else {
                    writeAndKeyword();
                }
            }
        }
    }


    public void visit(OWLTypedConstant node) {
        if (simpleRenderDatatypes.containsKey(node.getDataType().getURI())) {
            boolean renderQuotes = simpleRenderDatatypes.get(node.getDataType().getURI());
            if (renderQuotes) {
                write("\"");
            }
            write(node.getLiteral());
            if (renderQuotes) {
                write("\"");
            }
        }
        else {
            write("\"");
            write(node.getLiteral());
            write("\"^^");
            node.getDataType().accept(this);
        }
    }


    public void visit(OWLUntypedConstant node) {
        write("\"");
        write(node.getLiteral());
        write("\"");
        if (node.hasLang()) {
            write("@");
            write(node.getLang());
        }
    }


    public void visit(OWLDataType node) {
        write(node.getURI().getFragment());
    }


    public void visit(OWLDataOneOf node) {
        write("{");
        for (Iterator<OWLConstant> it = node.getValues().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(" ");
            }
        }
        write("}");
    }


    public void visit(OWLDataRangeRestriction node) {
//        writeOpenBracket(node);
        node.getDataRange().accept(this);
        write("[");
        for (Iterator<OWLDataRangeFacetRestriction> it = node.getFacetRestrictions().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(", ");
            }
        }
        write("]");
//        writeCloseBracket(node);
    }


    public void visit(OWLDataRangeFacetRestriction node) {
        String rendering = facetMap.get(node.getFacet());
        if (rendering == null) {
            rendering = node.getFacet().getShortName();
        }
        write(rendering);
        write(" ");
        node.getFacetValue().accept(this);
    }


    public void visit(OWLObjectSelfRestriction desc) {
        desc.getProperty().accept(this);
        write(" ");
        write(getSomeKeyWord());
        write(" Self");
    }


    public void visit(OWLDataAllRestriction node) {
        node.getProperty().accept(this);
        write(" ");
        write(getAllKeyWord());
        write(" ");
        node.getFiller().accept(this);
    }


    public void visit(OWLDataProperty node) {
        write(getRendering(node));
    }


    public void visit(OWLDataSomeRestriction node) {
        node.getProperty().accept(this);
        write(" ");
        write(getSomeKeyWord());
        write(" ");
        node.getFiller().accept(this);
    }


    public void visit(OWLDataValueRestriction node) {
        node.getProperty().accept(this);
        write(" ");
        write(getValueKeyWord());
        write(" ");
        node.getValue().accept(this);
    }


    public void visit(OWLIndividual node) {
        if (node.isAnonymous()) {
            write("Anonymous : [");
            for (OWLOntology ont : owlModelManager.getActiveOntologies()) {
                for (OWLDescription desc : node.getTypes(ont)) {
                    write(" ");
                    desc.accept(this);
                }
            }
            write(" ]");
        }
        else {
            write(getRendering(node));
        }
    }


    public void visit(OWLObjectAllRestriction node) {
        node.getProperty().accept(this);
        write(" ");
        write(getAllKeyWord());
        write(" ");
        writeOpenBracket(node.getFiller());
        node.getFiller().accept(this);
        writeCloseBracket(node.getFiller());
    }


    public void visit(OWLObjectMinCardinalityRestriction desc) {
        writeCardinality(desc, getMinKeyWord());
    }


    public void visit(OWLObjectExactCardinalityRestriction desc) {
        writeCardinality(desc, getExactlyKeyWord());
    }


    public void visit(OWLObjectMaxCardinalityRestriction desc) {
        writeCardinality(desc, getMaxKeyWord());
    }


    private void writeCardinality(OWLObjectCardinalityRestriction desc, String keyword) {
        desc.getProperty().accept(this);
        write(" ");
        write(keyword);
        write(" ");
        write(Integer.toString(desc.getCardinality()));
        write(" ");
        writeOpenBracket(desc.getFiller());
        desc.getFiller().accept(this);
        writeCloseBracket(desc.getFiller());
    }


    public void visit(OWLDataMinCardinalityRestriction desc) {
        writeCardinality(desc, getMinKeyWord());
    }


    public void visit(OWLDataExactCardinalityRestriction desc) {
        writeCardinality(desc, getExactlyKeyWord());
    }


    public void visit(OWLDataMaxCardinalityRestriction desc) {
        writeCardinality(desc, getMaxKeyWord());
    }


    private void writeCardinality(OWLDataCardinalityRestriction desc, String keyword) {
        desc.getProperty().accept(this);
        write(" ");
        write(keyword);
        write(" ");
        write(Integer.toString(desc.getCardinality()));
        write(" ");
        writeOpenBracket(desc.getFiller());
        desc.getFiller().accept(this);
        writeCloseBracket(desc.getFiller());
    }


    public void visit(OWLObjectProperty node) {
        write(getRendering(node));
    }


    public void visit(OWLObjectSomeRestriction node) {
        node.getProperty().accept(this);
        write(" ");
        write(getSomeKeyWord());
        write(" ");
        writeOpenBracket(node.getFiller());
        node.getFiller().accept(this);
        writeCloseBracket(node.getFiller());
    }


    public void visit(OWLObjectValueRestriction node) {
        node.getProperty().accept(this);
        write(" ");
        write(getValueKeyWord());
        write(" ");
        node.getValue().accept(this);
    }


    public void visit(OWLObjectComplementOf node) {
        writeNotKeyword();
        write(" ");
        writeOpenBracket(node.getOperand());
        node.getOperand().accept(this);
        writeCloseBracket(node.getOperand());
    }


    protected void writeNotKeyword() {
        write(getNotKeyWord());
    }


    public void visit(OWLObjectUnionOf node) {
        int indent = getIndent();
        for (Iterator it = sort(node.getOperands()).iterator(); it.hasNext();) {
            OWLDescription curOp = (OWLDescription) it.next();
            writeOpenBracket(curOp);
            curOp.accept(this);
            writeCloseBracket(curOp);
            if (it.hasNext()) {
                write("\n");
                insertIndent(indent);
                writeOrKeyword();
            }
        }
    }


    private void writeOrKeyword() {
        write(getOrKeyWord());
        write(" ");
    }


    public void visit(OWLClass node) {
        write(getRendering(node));
    }


    public void visit(OWLObjectPropertyInverse property) {
        write("inv(");
        property.getInverse().accept(this);
        write(")");
    }


    public void visit(OWLObjectOneOf node) {
        write("{");
        int size = node.getIndividuals().size();
        int count = 0;
        int indent = getIndent();
        for (Iterator<OWLIndividual> it = node.getIndividuals().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
//                write("\n");
//                insertIndent(indent);
                write(" ");
            }
        }
        write("}");
    }


    public void visit(OWLDisjointClassesAxiom node) {
        for (Iterator<OWLDescription> it = sort(node.getDescriptions()).iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(" disjointWith ");
            }
        }
    }


    public void visit(OWLEquivalentClassesAxiom node) {
        for (Iterator<OWLDescription> it = sort(node.getDescriptions()).iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(" equivalentTo ");
            }
        }
    }


    public void visit(OWLSubClassAxiom node) {
        node.getSubClass().accept(this);
        write(" subClassOf ");
        node.getSuperClass().accept(this);
    }


    public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
        write("Functional: ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
        write("InverseFunctional: ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
        write("Irreflexive: ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
        write("Reflexive: ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
        write("Symmetric: ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
        write("Transitive: ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLObjectPropertyDomainAxiom axiom) {
        axiom.getDomain().accept(this);
        write(" domainOf ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLObjectPropertyRangeAxiom axiom) {
        axiom.getRange().accept(this);
        write(" rangeOf ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLClassAssertionAxiom axiom) {
        axiom.getIndividual().accept(this);
        write(" instanceOf ");
        axiom.getDescription().accept(this);
    }


    public void visit(OWLFunctionalDataPropertyAxiom axiom) {
        write("Functional: ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLSameIndividualsAxiom axiom) {
        write("SameIndividuals: [");
        for (Iterator<OWLIndividual> it = axiom.getIndividuals().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(", ");
            }
        }
        write("]");
    }


    public void visit(OWLDifferentIndividualsAxiom axiom) {
        write("DifferentIndividuals: [");
        for (Iterator<OWLIndividual> it = axiom.getIndividuals().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(", ");
            }
        }
        write("]");
    }


    public void visit(OWLObjectPropertyAssertionAxiom axiom) {
        axiom.getSubject().accept(this);
        write(" ");
        axiom.getProperty().accept(this);
        write(" ");
        axiom.getObject().accept(this);
    }


    public void visit(OWLDataPropertyAssertionAxiom axiom) {
        axiom.getSubject().accept(this);
        write(" ");
        axiom.getProperty().accept(this);
        write(" ");
        axiom.getObject().accept(this);
    }


    public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
        write("not(");
        axiom.getSubject().accept(this);
        write(" ");
        axiom.getProperty().accept(this);
        write(" ");
        axiom.getObject().accept(this);
        write(")");
    }


    public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
        write("not(");
        axiom.getSubject().accept(this);
        write(" ");
        axiom.getProperty().accept(this);
        write(" ");
        axiom.getObject().accept(this);
        write(")");
    }


    public void visit(OWLInverseObjectPropertiesAxiom axiom) {
        axiom.getFirstProperty().accept(this);
        write(" inverseOf ");
        axiom.getSecondProperty().accept(this);
    }


    public void visit(OWLAntiSymmetricObjectPropertyAxiom axiom) {
        write("AntiSymmetric: ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLDataPropertyDomainAxiom axiom) {
        axiom.getDomain().accept(this);
        write(" domainOf ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLDataPropertyRangeAxiom axiom) {
        axiom.getRange().accept(this);
        write(" rangeOf ");
        axiom.getProperty().accept(this);
    }


    public void visit(OWLObjectSubPropertyAxiom axiom) {
        axiom.getSubProperty().accept(this);
        write(" subPropertyOf ");
        axiom.getSuperProperty().accept(this);
    }


    public void visit(OWLDisjointUnionAxiom axiom) {
        axiom.getOWLClass().accept(this);
        write(" disjointUnionOf ");
        write("[");
        int indent = getIndent();
        for (Iterator<OWLDescription> it = axiom.getDescriptions().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write("\n");
                insertIndent(indent);
            }
        }
        write("]");
    }


    public void visit(OWLImportsDeclaration axiom) {
        write(axiom.getImportedOntologyURI().toString());
    }


    private void writeOpenBracket(OWLDescription description) {
        description.accept(bracketWriter);
        if (bracketWriter.writeBrackets()) {
            write("(");
        }
    }


    private void writeOpenBracket(OWLDataRange dataRange) {
        dataRange.accept(bracketWriter);
        if (bracketWriter.writeBrackets()) {
            write("(");
        }
    }


    private void writeCloseBracket(OWLDescription description) {
        description.accept(bracketWriter);
        if (bracketWriter.writeBrackets()) {
            write(")");
        }
    }


    private void writeCloseBracket(OWLDataRange dataRange) {
        dataRange.accept(bracketWriter);
        if (bracketWriter.writeBrackets()) {
            write(")");
        }
    }


    public void visit(OWLOntology ontology) {
        write(ontology.getURI().toString());
    }


    public void visit(OWLObjectPropertyChainSubPropertyAxiom axiom) {
        for (Iterator<OWLObjectPropertyExpression> it = axiom.getPropertyChain().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(" o ");
            }
        }
        write(" \u279E ");
        axiom.getSuperProperty().accept(this);
    }


    public void visit(OWLConstantAnnotation annotation) {
        write(annotation.getAnnotationURI().toString());
        write(annotation.getAnnotationValue().toString());
    }


    public void visit(SWRLRule swrlRule) {
        for (Iterator<SWRLAtom> it = swrlRule.getBody().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(" \u2227 ");
            }
        }
        write(" \u2192 ");
        for (Iterator<SWRLAtom> it = swrlRule.getHead().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(" \u2227 ");
            }
        }
    }


    public void visit(SWRLClassAtom swrlClassAtom) {
        OWLDescription desc = swrlClassAtom.getPredicate();
        if (desc.isAnonymous()) {
            write("(");
        }
        desc.accept(this);
        if (desc.isAnonymous()) {
            write(")");
        }
        write("(");
        swrlClassAtom.getArgument().accept(this);
        write(")");
    }


    public void visit(SWRLDataRangeAtom swrlDataRangeAtom) {
        swrlDataRangeAtom.getPredicate().accept(this);
        write("(");
        swrlDataRangeAtom.getArgument().accept(this);
        write(")");
    }


    public void visit(SWRLObjectPropertyAtom swrlObjectPropertyAtom) {
        swrlObjectPropertyAtom.getPredicate().accept(this);
        write("(");
        swrlObjectPropertyAtom.getFirstArgument().accept(this);
        write(", ");
        swrlObjectPropertyAtom.getSecondArgument().accept(this);
        write(")");
    }


    public void visit(SWRLDataValuedPropertyAtom swrlDataValuedPropertyAtom) {
        swrlDataValuedPropertyAtom.getPredicate().accept(this);
        write("(");
        swrlDataValuedPropertyAtom.getFirstArgument().accept(this);
        write(", ");
        swrlDataValuedPropertyAtom.getSecondArgument().accept(this);
        write(")");
    }


    public void visit(SWRLBuiltInAtom swrlBuiltInAtom) {
        super.visit(swrlBuiltInAtom);
    }


    public void visit(SWRLAtomDVariable swrlAtomDVariable) {
        write("?");
        write(swrlAtomDVariable.getURI().getFragment());
    }


    public void visit(SWRLAtomIVariable swrlAtomIVariable) {
        write("?");
        write(swrlAtomIVariable.getURI().getFragment());
    }


    public void visit(SWRLAtomIndividualObject swrlAtomIndividualObject) {
        swrlAtomIndividualObject.getIndividual().accept(this);
    }


    public void visit(SWRLAtomConstantObject swrlAtomConstantObject) {
        swrlAtomConstantObject.getConstant().accept(this);
    }


    public void visit(SWRLDifferentFromAtom swrlDifferentFromAtom) {
        swrlDifferentFromAtom.getPredicate().accept(this);
        write("(");
        swrlDifferentFromAtom.getFirstArgument().accept(this);
        write(", ");
        swrlDifferentFromAtom.getSecondArgument().accept(this);
        write(")");
    }


    public void visit(SWRLSameAsAtom swrlSameAsAtom) {
        swrlSameAsAtom.getPredicate().accept(this);
        write("(");
        swrlSameAsAtom.getFirstArgument().accept(this);
        write(", ");
        swrlSameAsAtom.getSecondArgument().accept(this);
        write(")");
    }


    private class BracketWriter extends OWLDescriptionVisitorAdapter implements OWLDataVisitor {

        boolean nested = false;


        public boolean writeBrackets() {
            return nested;
        }


        public void visit(OWLObjectIntersectionOf owlAnd) {
            nested = true;
        }


        public void visit(OWLDataAllRestriction owlDataAllRestriction) {
            nested = true;
        }


        public void visit(OWLDataSomeRestriction owlDataSomeRestriction) {
            nested = true;
        }


        public void visit(OWLDataValueRestriction owlDataValueRestriction) {
            nested = true;
        }


        public void visit(OWLObjectAllRestriction owlObjectAllRestriction) {
            nested = true;
        }


        public void visit(OWLObjectSomeRestriction owlObjectSomeRestriction) {
            nested = true;
        }


        public void visit(OWLObjectValueRestriction owlObjectValueRestriction) {
            nested = true;
        }


        public void visit(OWLObjectComplementOf owlNot) {
            nested = true;
        }


        public void visit(OWLObjectUnionOf owlOr) {
            nested = true;
        }


        public void visit(OWLClass owlClass) {
            nested = false;
        }


        public void visit(OWLObjectOneOf owlObjectOneOf) {
            nested = false;
        }


        public void visit(OWLObjectMinCardinalityRestriction desc) {
            nested = true;
        }


        public void visit(OWLObjectExactCardinalityRestriction desc) {
            nested = true;
        }


        public void visit(OWLObjectMaxCardinalityRestriction desc) {
            nested = true;
        }


        public void visit(OWLObjectSelfRestriction desc) {
            nested = true;
        }


        public void visit(OWLDataMinCardinalityRestriction desc) {
            nested = true;
        }


        public void visit(OWLDataExactCardinalityRestriction desc) {
            nested = true;
        }


        public void visit(OWLDataMaxCardinalityRestriction desc) {
            nested = true;
        }


        public void visit(OWLDataType node) {
            nested = false;
        }


        public void visit(OWLDataComplementOf node) {
            nested = false;
        }


        public void visit(OWLDataOneOf node) {
            nested = false;
        }


        public void visit(OWLDataRangeRestriction node) {
            nested = true;
        }


        public void visit(OWLTypedConstant node) {
            nested = false;
        }


        public void visit(OWLUntypedConstant node) {
            nested = false;
        }


        public void visit(OWLDataRangeFacetRestriction node) {
            nested = false;
        }
    }
}
