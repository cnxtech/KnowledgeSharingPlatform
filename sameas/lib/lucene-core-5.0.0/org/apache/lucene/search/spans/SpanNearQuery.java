package org.apache.lucene.search.spans;

/*
 *
 * Copyright(c) 2015, Samsung Electronics Co., Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.
    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import java.io.IOException;


import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.ToStringUtils;

/** Matches spans which are near one another.  One can specify <i>slop</i>, the
 * maximum number of intervening unmatched positions, as well as whether
 * matches are required to be in-order. */
public class SpanNearQuery extends SpanQuery implements Cloneable {
  protected List<SpanQuery> clauses;
  protected int slop;
  protected boolean inOrder;

  protected String field;
  private boolean collectPayloads;

  /** Construct a SpanNearQuery.  Matches spans matching a span from each
   * clause, with up to <code>slop</code> total unmatched positions between
   * them.
   * <br>When <code>inOrder</code> is true, the spans from each clause
   * must be in the same order as in <code>clauses</code> and must be non-overlapping.
   * <br>When <code>inOrder</code> is false, the spans from each clause
   * need not be ordered and may overlap.
   * @param clauses the clauses to find near each other
   * @param slop The slop value
   * @param inOrder true if order is important
   */
  public SpanNearQuery(SpanQuery[] clauses, int slop, boolean inOrder) {
    this(clauses, slop, inOrder, true);     
  }
  
  public SpanNearQuery(SpanQuery[] clauses, int slop, boolean inOrder, boolean collectPayloads) {

    // copy clauses array into an ArrayList
    this.clauses = new ArrayList<>(clauses.length);
    for (int i = 0; i < clauses.length; i++) {
      SpanQuery clause = clauses[i];
      if (field == null) {                               // check field
        field = clause.getField();
      } else if (clause.getField() != null && !clause.getField().equals(field)) {
        throw new IllegalArgumentException("Clauses must have same field.");
      }
      this.clauses.add(clause);
    }
    this.collectPayloads = collectPayloads;
    this.slop = slop;
    this.inOrder = inOrder;
  }

  /** Return the clauses whose spans are matched. */
  public SpanQuery[] getClauses() {
    return clauses.toArray(new SpanQuery[clauses.size()]);
  }

  /** Return the maximum number of intervening unmatched positions permitted.*/
  public int getSlop() { return slop; }

  /** Return true if matches are required to be in-order.*/
  public boolean isInOrder() { return inOrder; }

  @Override
  public String getField() { return field; }
  
  @Override
  public void extractTerms(Set<Term> terms) {
    for (final SpanQuery clause : clauses) {
      clause.extractTerms(terms);
    }
  }  
  

  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("spanNear([");
    Iterator<SpanQuery> i = clauses.iterator();
    while (i.hasNext()) {
      SpanQuery clause = i.next();
      buffer.append(clause.toString(field));
      if (i.hasNext()) {
        buffer.append(", ");
      }
    }
    buffer.append("], ");
    buffer.append(slop);
    buffer.append(", ");
    buffer.append(inOrder);
    buffer.append(")");
    buffer.append(ToStringUtils.boost(getBoost()));
    return buffer.toString();
  }

  @Override
  public Spans getSpans(final LeafReaderContext context, Bits acceptDocs, Map<Term,TermContext> termContexts) throws IOException {
    if (clauses.size() == 0)                      // optimize 0-clause case
      return new SpanOrQuery(getClauses()).getSpans(context, acceptDocs, termContexts);

    if (clauses.size() == 1)                      // optimize 1-clause case
      return clauses.get(0).getSpans(context, acceptDocs, termContexts);

    return inOrder
            ? (Spans) new NearSpansOrdered(this, context, acceptDocs, termContexts, collectPayloads)
            : (Spans) new NearSpansUnordered(this, context, acceptDocs, termContexts);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    SpanNearQuery clone = null;
    for (int i = 0 ; i < clauses.size(); i++) {
      SpanQuery c = clauses.get(i);
      SpanQuery query = (SpanQuery) c.rewrite(reader);
      if (query != c) {                     // clause rewrote: must clone
        if (clone == null)
          clone = this.clone();
        clone.clauses.set(i,query);
      }
    }
    if (clone != null) {
      return clone;                        // some clauses rewrote
    } else {
      return this;                         // no clauses rewrote
    }
  }
  
  @Override
  public SpanNearQuery clone() {
    int sz = clauses.size();
    SpanQuery[] newClauses = new SpanQuery[sz];

    for (int i = 0; i < sz; i++) {
      newClauses[i] = (SpanQuery) clauses.get(i).clone();
    }
    SpanNearQuery spanNearQuery = new SpanNearQuery(newClauses, slop, inOrder);
    spanNearQuery.setBoost(getBoost());
    return spanNearQuery;
  }

  /** Returns true iff <code>o</code> is equal to this. */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SpanNearQuery)) return false;

    final SpanNearQuery spanNearQuery = (SpanNearQuery) o;

    if (inOrder != spanNearQuery.inOrder) return false;
    if (slop != spanNearQuery.slop) return false;
    if (!clauses.equals(spanNearQuery.clauses)) return false;

    return getBoost() == spanNearQuery.getBoost();
  }

  @Override
  public int hashCode() {
    int result;
    result = clauses.hashCode();
    // Mix bits before folding in things like boost, since it could cancel the
    // last element of clauses.  This particular mix also serves to
    // differentiate SpanNearQuery hashcodes from others.
    result ^= (result << 14) | (result >>> 19);  // reversible
    result += Float.floatToRawIntBits(getBoost());
    result += slop;
    result ^= (inOrder ? 0x99AFD3BD : 0);
    return result;
  }
}