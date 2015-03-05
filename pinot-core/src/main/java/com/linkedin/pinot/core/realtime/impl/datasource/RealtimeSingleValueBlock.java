package com.linkedin.pinot.core.realtime.impl.datasource;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.roaringbitmap.buffer.MutableRoaringBitmap;

import com.linkedin.pinot.common.data.FieldSpec;
import com.linkedin.pinot.common.data.FieldSpec.DataType;
import com.linkedin.pinot.common.data.FieldSpec.FieldType;
import com.linkedin.pinot.common.data.Schema;
import com.linkedin.pinot.core.common.Block;
import com.linkedin.pinot.core.common.BlockDocIdSet;
import com.linkedin.pinot.core.common.BlockDocIdValueSet;
import com.linkedin.pinot.core.common.BlockId;
import com.linkedin.pinot.core.common.BlockMetadata;
import com.linkedin.pinot.core.common.BlockSingleValIterator;
import com.linkedin.pinot.core.common.BlockValIterator;
import com.linkedin.pinot.core.common.BlockValSet;
import com.linkedin.pinot.core.common.Constants;
import com.linkedin.pinot.core.common.Predicate;
import com.linkedin.pinot.core.realtime.impl.dictionary.MutableDictionaryReader;
import com.linkedin.pinot.core.realtime.impl.fwdindex.DimensionTuple;
import com.linkedin.pinot.core.realtime.utils.RealtimeDimensionsSerDe;
import com.linkedin.pinot.core.realtime.utils.RealtimeMetricsSerDe;
import com.linkedin.pinot.core.segment.index.block.BlockUtils;


public class RealtimeSingleValueBlock implements Block {

  private final MutableRoaringBitmap filteredBitmap;
  private final FieldSpec spec;
  private final MutableDictionaryReader dictionary;
  private final Map<Object, Pair<Long, Long>> docIdMap;
  private final String columnName;
  private final int docIdSearchableOffset;
  private final Schema schema;
  private Predicate p;
  private final Map<Long, DimensionTuple> dimemsionTupleMap;
  private final RealtimeDimensionsSerDe dimSerDe;
  private final RealtimeMetricsSerDe metSerDe;

  public RealtimeSingleValueBlock(FieldSpec spec, MutableDictionaryReader dictionary,
      Map<Object, Pair<Long, Long>> docIdMap, MutableRoaringBitmap filteredDocids, String columnName, int docIdOffset,
      Schema schema, Map<Long, DimensionTuple> dimemsionTupleMap, RealtimeDimensionsSerDe dimSerDe,
      RealtimeMetricsSerDe metSerde) {
    this.spec = spec;
    this.dictionary = dictionary;
    this.filteredBitmap = filteredDocids;
    this.docIdMap = docIdMap;
    this.columnName = columnName;
    this.docIdSearchableOffset = docIdOffset;
    this.schema = schema;
    this.dimemsionTupleMap = dimemsionTupleMap;
    this.dimSerDe = dimSerDe;
    this.metSerDe = metSerde;
  }

  @Override
  public BlockId getId() {
    return null;
  }

  @Override
  public boolean applyPredicate(Predicate predicate) {
    this.p = predicate;
    return true;
  }

  @Override
  public BlockDocIdSet getBlockDocIdSet() {
    if (this.p != null) {
      return BlockUtils.getBLockDocIdSetBackedByBitmap(filteredBitmap);
    }

    return BlockUtils.getDummyBlockDocIdSet(docIdSearchableOffset);
  }

  @Override
  public BlockValSet getBlockValueSet() {
    if (spec.getFieldType() == FieldType.dimension) {
      return getDimensionBlockValueSet();
    } else if (spec.getFieldType() == FieldType.metric) {
      return getMetricBlockValueSet();
    }
    return getTimeBlockValueSet();
  }

  private BlockValSet getDimensionBlockValueSet() {
    return new BlockValSet() {

      @Override
      public BlockValIterator iterator() {
        return new BlockSingleValIterator() {
          private int counter = 0;
          private int max = docIdSearchableOffset;

          @Override
          public boolean skipTo(int docId) {
            if (docId > max) {
              return false;
            }
            counter = docId;
            return true;
          }

          @Override
          public int size() {
            return max;
          }

          @Override
          public boolean reset() {
            counter = 0;
            return true;
          }

          @Override
          public boolean next() {
            counter++;
            return counter < max;
          }

          @Override
          public int nextIntVal() {
            if (counter > max) {
              return Constants.EOF;
            }

            Pair<Long, Long> documentFinderPair = docIdMap.get(counter);
            long hash64 = documentFinderPair.getLeft();
            DimensionTuple tuple = dimemsionTupleMap.get(hash64);
            IntBuffer rawData = tuple.getDimBuff();
            int vals[] = dimSerDe.deSerializeAndReturnDicIdsFor(columnName, rawData);
            counter++;
            return vals[0];
          }

          @Override
          public boolean hasNext() {
            return (counter < max);
          }

          @Override
          public DataType getValueType() {
            return spec.getDataType();
          }

          @Override
          public int currentDocId() {
            return counter;
          }
        };
      }

      @Override
      public DataType getValueType() {
        return spec.getDataType();
      }
    };
  }

  private BlockValSet getTimeBlockValueSet() {
    return new BlockValSet() {

      @Override
      public BlockValIterator iterator() {
        return new BlockSingleValIterator() {
          private int counter = 0;
          private int max = docIdSearchableOffset;

          @Override
          public boolean skipTo(int docId) {
            if (docId > max) {
              return false;
            }
            counter = docId;
            return true;
          }

          @Override
          public int size() {
            return max;
          }

          @Override
          public boolean reset() {
            counter = 0;
            return true;
          }

          @Override
          public boolean next() {
            counter++;
            return counter > max;
          }

          @Override
          public long nextLongVal() {
            if (counter >= max) {
              return Constants.EOF;
            }

            Pair<Long, Long> documentFinderPair = docIdMap.get(counter);
            return documentFinderPair.getRight();
          }

          @Override
          public boolean hasNext() {
            return (counter <= max);
          }

          @Override
          public DataType getValueType() {
            return spec.getDataType();
          }

          @Override
          public int currentDocId() {
            return counter;
          }
        };
      }

      @Override
      public DataType getValueType() {
        return spec.getDataType();
      }
    };
  }

  private BlockValSet getMetricBlockValueSet() {
    return new BlockValSet() {

      @Override
      public BlockValIterator iterator() {
        return new BlockSingleValIterator() {
          private int counter = 0;
          private int max = docIdSearchableOffset;

          @Override
          public boolean skipTo(int docId) {
            if (docId > max) {
              return false;
            }
            counter = docId;
            return true;
          }

          @Override
          public int size() {
            return max;
          }

          @Override
          public boolean reset() {
            counter = 0;
            return true;
          }

          @Override
          public boolean next() {
            counter++;
            return counter > max;
          }

          @Override
          public int nextIntVal() {
            if (counter >= max) {
              return Constants.EOF;
            }

            Pair<Long, Long> documentFinderPair = docIdMap.get(counter);
            long hash64 = documentFinderPair.getLeft();
            DimensionTuple tuple = dimemsionTupleMap.get(hash64);
            ByteBuffer rawData = tuple.getMetricsBuffForTime(documentFinderPair.getRight());
            return metSerDe.getIntVal(columnName, rawData);
          }

          @Override
          public long nextLongVal() {
            if (counter >= max) {
              return Constants.EOF;
            }

            Pair<Long, Long> documentFinderPair = docIdMap.get(counter);
            long hash64 = documentFinderPair.getLeft();
            DimensionTuple tuple = dimemsionTupleMap.get(hash64);
            ByteBuffer rawData = tuple.getMetricsBuffForTime(documentFinderPair.getRight());
            return metSerDe.getLongVal(columnName, rawData);
          }

          @Override
          public float nextFloatVal() {
            if (counter >= max) {
              return Constants.EOF;
            }

            Pair<Long, Long> documentFinderPair = docIdMap.get(counter);
            long hash64 = documentFinderPair.getLeft();
            DimensionTuple tuple = dimemsionTupleMap.get(hash64);
            ByteBuffer rawData = tuple.getMetricsBuffForTime(documentFinderPair.getRight());
            return metSerDe.getFloatVal(columnName, rawData);
          }

          @Override
          public double nextDoubleVal() {
            if (counter >= max) {
              return Constants.EOF;
            }

            Pair<Long, Long> documentFinderPair = docIdMap.get(counter);
            long hash64 = documentFinderPair.getLeft();
            DimensionTuple tuple = dimemsionTupleMap.get(hash64);
            ByteBuffer rawData = tuple.getMetricsBuffForTime(documentFinderPair.getRight());
            return metSerDe.getDoubleVal(columnName, rawData);
          }

          @Override
          public boolean hasNext() {
            return (counter <= max);
          }

          @Override
          public DataType getValueType() {
            return spec.getDataType();
          }

          @Override
          public int currentDocId() {
            return counter;
          }
        };
      }

      @Override
      public DataType getValueType() {
        return spec.getDataType();
      }
    };
  }

  @Override
  public BlockDocIdValueSet getBlockDocIdValueSet() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public BlockMetadata getMetadata() {
    if (spec.getFieldType() == FieldType.dimension) {
      return getDimensionBlockMetadata();
    }

    return getBlockMetadataForMetricsOrTimeColumn();
  }

  private BlockMetadata getDimensionBlockMetadata() {
    return new BlockMetadata() {

      @Override
      public int maxNumberOfMultiValues() {
        return 0;
      }

      @Override
      public boolean isSparse() {
        return false;
      }

      @Override
      public boolean isSorted() {
        return false;
      }

      @Override
      public boolean isSingleValue() {
        return true;
      }

      @Override
      public boolean hasInvertedIndex() {
        return true;
      }

      @Override
      public boolean hasDictionary() {
        return true;
      }

      @Override
      public int getStartDocId() {
        return 0;
      }

      @Override
      public int getSize() {
        return docIdSearchableOffset;
      }

      @Override
      public int getLength() {
        // TODO Auto-generated method stub
        return docIdSearchableOffset;
      }

      @Override
      public int getEndDocId() {
        // TODO Auto-generated method stub
        return docIdSearchableOffset;
      }

      @Override
      public com.linkedin.pinot.core.segment.index.readers.Dictionary getDictionary() {
        return dictionary;
      }

      @Override
      public DataType getDataType() {
        return spec.getDataType();
      }
    };
  }

  private BlockMetadata getBlockMetadataForMetricsOrTimeColumn() {
    return new BlockMetadata() {

      @Override
      public int maxNumberOfMultiValues() {
        return -1;
      }

      @Override
      public boolean isSparse() {
        return false;
      }

      @Override
      public boolean isSorted() {
        return false;
      }

      @Override
      public boolean isSingleValue() {
        return true;
      }

      @Override
      public boolean hasInvertedIndex() {
        return true;
      }

      @Override
      public boolean hasDictionary() {
        return false;
      }

      @Override
      public int getStartDocId() {
        return 0;
      }

      @Override
      public int getSize() {
        return docIdSearchableOffset;
      }

      @Override
      public int getLength() {
        return docIdSearchableOffset;
      }

      @Override
      public int getEndDocId() {
        return docIdSearchableOffset;
      }

      @Override
      public com.linkedin.pinot.core.segment.index.readers.Dictionary getDictionary() {
        return null;
      }

      @Override
      public DataType getDataType() {
        return spec.getDataType();
      }
    };
  }
}