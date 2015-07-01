package org.apache.lucene.document;

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

import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.util.NumericUtils;

/**
 * <p>
 * Field that stores a per-document <code>long</code> values for scoring, 
 * sorting or value retrieval. Here's an example usage:
 * 
 * <pre class="prettyprint">
 *   document.add(new SortedNumericDocValuesField(name, 5L));
 *   document.add(new SortedNumericDocValuesField(name, 14L));
 * </pre>
 * 
 * <p>
 * Note that if you want to encode doubles or floats with proper sort order,
 * you will need to encode them with {@link NumericUtils}:
 * 
 * <pre class="prettyprint">
 *   document.add(new SortedNumericDocValuesField(name, NumericUtils.floatToSortableInt(-5.3f)));
 * </pre>
 * 
 * <p>
 * If you also need to store the value, you should add a
 * separate {@link StoredField} instance.
 * */

public class SortedNumericDocValuesField extends Field {

  /**
   * Type for sorted numeric DocValues.
   */
  public static final FieldType TYPE = new FieldType();
  static {
    TYPE.setDocValuesType(DocValuesType.SORTED_NUMERIC);
    TYPE.freeze();
  }

  /** 
   * Creates a new DocValues field with the specified 64-bit long value 
   * @param name field name
   * @param value 64-bit long value
   * @throws IllegalArgumentException if the field name is null
   */
  public SortedNumericDocValuesField(String name, long value) {
    super(name, TYPE);
    fieldsData = Long.valueOf(value);
  }
}