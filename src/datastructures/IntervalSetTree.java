package datastructures;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A balanced binary-search tree keyed by Interval objects.
 * <p>
 * The underlying data-structure is a red-black tree largely implemented from
 * CLRS (Introduction to Algorithms, 2nd edition) with the interval-tree
 * extensions mentioned in section 14.3
 * @param <I> - the type of Interval this tree contains
 */
public class IntervalSetTree<T extends Interval> implements Iterable<T> {

    private Node root;  // The root Node.
    private Node nil;   // The sentinel Node to represent the absence of a node.
    private int size;   // Size of the tree. Updated by insert() and delete()

    /**
     * Constructs an empty IntervalTree.
     */
    public IntervalSetTree() {
        nil = new Node();
        root = nil;
        size = 0;
    }
    
    /**
     * Constructs an IntervalTree containing a single node corresponding to
     * the given interval.
     * @param t - the interval to add to the tree
     */
    public IntervalSetTree(T t) {
        nil = new Node();
        root = new Node(t);
        root.blacken();
        size = 1;
    }

    ///////////////////////////////////
    // Tree -- General query methods //
    ///////////////////////////////////
    
    /**
     * Whether this IntervalTree is empty or not.
     */
    public boolean isEmpty() {
        return root.isNil();
    }
    
    /**
     * The number of intervals stored in this IntervalTree.
     */
    public int size() {
        return size;
    }
    
    /**
     * The Node in this IntervalTree that contains the given Interval.
     * <p>
     * This method returns the nil Node if the Interval t cannot be found.
     * @param t - the Interval to search for.
     */
    private Node search(T t) {
        return root.search(t);
    }
    
    /**
     * Whether or not this IntervalTree contains the given Interval.
     * @param t - the Interval to search for
     */
    public boolean contains(T t) {
        return search(t).intervals.contains(t);
    }
    
    /**
     * The minimum value in this IntervalTree
     * @return an Optional containing, if it exists, the minimum value in this
     * IntervalTree; otherwise (i.e., if this is empty), an empty Optional.
     */
    public Iterator<T> minimum() {
        Node n = root.minimumNode();
        return n.isNil() ? Collections.emptyIterator() : n.intervals.iterator();
    }

    /**
     * The maximum value in this IntervalTree
     * @return an Optional containing, if it exists, the maximum value in this
     * IntervalTree; otherwise (i.e., if this is empty), an empty Optional.
     */
    public Iterator<T> maximum() {
        Node n = root.maximumNode();
        return n.isNil() ? Collections.emptyIterator() : n.intervals.iterator();
    }
    
    /**
     * The next Interval in this IntervalTree
     * @param t - the Interval to search for
     * @return an Optional containing, if it exists, the next Interval in this
     * IntervalTree; otherwise (if t is the maximum Interval, or if this
     * IntervalTree does not contain t), an empty Optional.
     */
    public Iterator<T> successors(T t) {
        Node n = search(t);
        if (n.isNil()) {
            return Collections.emptyIterator();
        }

        n = n.successor();
        if (n.isNil()) {
            return Collections.emptyIterator();
        }

        return n.intervals.iterator();
    }

    /**
     * The previous Interval in this IntervalTree
     * @param t - the Interval to search for
     * @return an Optional containing, if it exists, the previous Interval in
     * this IntervalTree; otherwise (if t is the minimum Interval, or if this
     * IntervalTree does not contain t), an empty Optional.
     */
    public Iterator<T> predecessors(T t) {
        Node n = search(t);
        if (n.isNil()) {
            return Collections.emptyIterator();
        }

        n = n.predecessor();
        if (n.isNil()) {
            return Collections.emptyIterator();
        }

        return n.intervals.iterator();
    }

    /**
     * An Iterator which traverses the tree in ascending order.
     */
    public Iterator<T> iterator() {
        return new TreeIterator(root);
    }
    
    /**
     * An Iterator over the Intervals in this IntervalTree that overlap the
     * given Interval
     * @param t - the overlapping Interval
     */
    public Iterator<T> overlappers(T t) {
        return root.overlappers(t);
    }
    
    /**
     * Whether or not any of the Intervals in this IntervalTree overlap the given
     * Interval
     * @param t - the potentially overlapping Interval
     */
    public boolean overlaps(T t) {
        return !root.anyOverlappingNode(t).isNil();
    }
    
    /**
     * The number of Intervals in this IntervalTree that overlap the given
     * Interval
     * @param t - the overlapping Interval
     */
    public int numOverlappers(T t) {
        return root.numOverlappingIntervals(t);
    }
    
    /**
     * The least Interval in this IntervalTree that overlaps the given Interval
     * @param t - the overlapping Interval
     * @return an Optional containing, if it exists, the least Interval in this
     * IntervalTree that overlaps the given Interval; otherwise (i.e., if there
     * is no overlap), an empty Optional
     */
    public Iterator<T> minimumOverlappers(T t) {
        Node n = root.minimumOverlappingNode(t);
        return n.isNil() ? Collections.emptyIterator() : n.intervals.iterator();
    }
    
    ///////////////////////////////
    // Tree -- Insertion methods //
    ///////////////////////////////

    /**
     * Inserts the given value into the IntervalTree.
     * <p>
     * This method constructs a new Node containing the given value and places
     * it into the tree. If the value already exists within the tree, the tree
     * remains unchanged.
     * @param t - the value to place into the tree
     * @return if the value did not already exist, i.e., true if the tree was
     * changed, false if it was not
     */
    public boolean insert(T t) {
        
        Node y = nil;
        Node x = root;

        // Traverse the tree down to a leaf
        while (!x.isNil()) {
            y = x;
            
            // Update maxEnd on the way down.
            x.maxEnd = Math.max(x.maxEnd, t.end());

            // If the Node for this Interval already exists, add the Interval
            // to its Set and increment size if successful.
            int cmp = t.compareTo(x);
            if (cmp == 0) {
                if (x.intervals.add(t)) {
                    size++;
                    return true;
                } else {
                    return false;
                }
            }
            x = cmp == -1 ? x.left : x.right;
        }

        // Didn't find the correct Node on the way down, so make a new Node
        // containing the Interval.
        Node z = new Node(t);
        z.parent = y;

        if (y.isNil()) {                // Three cases:
            root = z;
            root.blacken();             // 1) New node is root
        } else {
            int cmp = z.compareTo(y);
            if (cmp == -1) {
                y.left = z;             // 2) New node is left-child leaf
            } else {
                assert(cmp == 1);
                y.right = z;            // 3) New node is right-child leaf
            }
            
            z.left = nil;
            z.right = nil;
            z.redden();
            z.insertFixup();
        }
        size++;
        return true;
    }
    
    //////////////////////////////
    // Tree -- Deletion methods //
    //////////////////////////////
    
    /**
     * Deletes the given value from this IntervalTree.
     * <p>
     * If the value does not exist, this IntervalTree remains unchanged. 
     * @param t - the Interval to delete from the tree
     * @return whether or not an Interval was removed from this IntervalTree
     */
    public boolean delete(T t) {
        Node n = search(t);
        boolean rtrn = n.intervals.remove(t);
        if (rtrn) {
            size--;
        }
        if (n.intervals.isEmpty()) {
            n.delete(); // Node#delete does nothing if n.isNil()
        }
        return rtrn;
    }
    
    public boolean deleteSameBounds(T t) {
        return search(t).delete();
    }
    
    /**
     * Deletes the smallest Interval from this IntervalTree.
     * <p>
     * If there is no smallest Interval (that is, if the tree is empty), this
     * IntervalTree remains unchanged.
     * @return whether or not an Interval was removed from this IntervalTree
     */
    public boolean deleteMin() {            // Node#delete does nothing and
        return root.minimumNode().delete(); // returns false if t.isNil()
    }
    
    /**
     * Deletes the greatest Interval from this IntervalTree.
     * <p>
     * If there is no greatest Interval (that is, if the tree is empty), this
     * IntervalTree remains unchanged.
     * @return whether or not an Interval was removed from this IntervalTree
     */
    public boolean deleteMax() {            // Node#delete does nothing and
        return root.maximumNode().delete(); // returns false if t.isNil()
    }
    
    /**
     * Deletes all Intervals that overlap the given Interval from this
     * IntervalTree.
     * <p>
     * If there are no overlapping Intervals, this IntervalTree remains
     * unchanged.
     * @param t - the overlapping Interval
     * @return whether or not an Interval was removed from this IntervalTree
     */
    public boolean deleteOverlappers(T t) {
        // TODO 
        // Replacing the line
        //    s.forEach(n -> delete(n.interval()))
        // with
        //    s.forEach(n -> n.delete())
        // causes a NullPointerException in resetMaxEnd(). Why?!
        //
        // As it stands, every deletion operation causes the tree
        // to be searched. Fix this, please.

        Set<Node> s = new HashSet<Node>();
        Iterator<Node> iter = new OverlappingNodeIterator(root, t);
        iter.forEachRemaining(s::add);
        return s.stream()
                .map(n -> deleteSameBounds(n.intervals.iterator().next()))
                .reduce(false, (a, b) -> a || b);
    }

    /**
     * A representation of a node in an interval tree.
     */
    private class Node implements Interval, Iterable<T> {
        
        /* Most of the "guts" of the interval tree are actually methods called
         * by nodes. For example, IntervalTree#delete(val) searches up the Node
         * containing val; then that Node deletes itself with Node#delete().
         */

        private Set<T> intervals;
        private int start;
        private int end;
        private Node parent;
        private Node left;
        private Node right;
        private boolean isBlack;
        private int maxEnd;

        /**
         * Constructs a Node with no data.
         * <p>
         * This Node has a null interval field, is black, and has all pointers
         * pointing at itself. This is intended to be used as the sentinel
         * node in the tree ("nil" in CLRS).
         */
        private Node() {
            intervals = Collections.emptySet();
            parent = this;
            left = this;
            right = this;
            blacken();
        }
        
        /**
         * Constructs a Node containing the given Interval.
         * @param data - the Interval to be contained within this Node
         */
        public Node(T interval) {
            intervals = new HashSet<T>();
            intervals.add(interval);
            parent = nil;
            left = nil;
            right = nil;
            start = interval.start();
            end = interval.end();
            maxEnd = end;
            redden();
        }
        
        public Iterator<T> iterator() {
            return intervals.iterator();
        }
        
        /**
         * The start of the Interval in this Node
         */
        @Override
        public int start() {
            return start;
        }

        /**
         * The end of the Interval in this Node
         */
        @Override
        public int end() {
            return end;
        }
        
        ///////////////////////////////////
        // Node -- General query methods //
        ///////////////////////////////////
        
        /**
         * Searches the subtree rooted at this Node for the given Interval.
         * @param t - the Interval to search for
         * @return the Node with the given Interval, if it exists; otherwise,
         * the sentinel Node 
         */
        private Node search(T t) {

            Node n = this;
            
            while (!n.isNil() && t.compareTo(n) != 0) {
                n = t.compareTo(n) == -1 ? n.left : n.right;
            }
            return n;
        }

        /**
         * Searches the subtree rooted at this Node for its minimum Interval.
         * @return the Node with the minimum Interval, if it exists; otherwise,
         * the sentinel Node
         */
        private Node minimumNode() {
            
            Node n = this;
            
            while (!n.left.isNil()) {
                n = n.left;
            }
            return n;
        }

        /**
         * Searches the subtree rooted at this Node for its maximum Interval.
         * @return the Node with the maximum Interval, if it exists; otherwise,
         * the sentinel Node
         */
        private Node maximumNode() {
            
            Node n = this;
            
            while (!n.right.isNil()) {
                n = n.right;
            }
            return n;
        }
        
        /**
         * The successor of this Node.
         * @return the Node following this Node, if it exists; otherwise the
         * sentinel Node
         */
        private Node successor() {
            
            if (!right.isNil()) {
                return right.minimumNode();
            }
            
            Node x = this;
            Node y = parent;
            while (!y.isNil() && x == y.right) {
                x = y;
                y = y.parent;
            }
            
            return y;
        }

        /**
         * The predecessor of this Node.
         * @return the Node preceding this Node, if it exists; otherwise the
         * sentinel Node
         */
        private Node predecessor() {
            
            if (!left.isNil()) {
                return left.maximumNode();
            }
            
            Node x = this;
            Node y = parent;
            while (!y.isNil() && x == y.left) {
                x = y;
                y = y.parent;
            }
            
            return y;
        }
        
        ///////////////////////////////////////
        // Node -- Overlapping query methods //
        ///////////////////////////////////////
        
        /**
         * Returns a Node from this Node's subtree that overlaps the given
         * Interval.
         * <p>
         * The only guarantee of this method is that the returned Node overlaps
         * the Interval t. This method is meant to be a quick helper method to
         * determine if any overlap exists between an Interval and any of an
         * IntervalTree's Intervals. The returned Node will be the first
         * overlapping one found.
         * @param t - the given Interval
         * @return an overlapping Node from this Node's subtree, if one exists;
         * otherwise the sentinel Node
         */
        private Node anyOverlappingNode(T t) {
            Node x = this;
            while (!x.isNil() && !t.overlaps(x)) {
                x = !x.left.isNil() && x.left.maxEnd > t.start() ? x.left : x.right;
            }
            return x;
        }
        
        /**
         * Returns the minimum Node from this Node's subtree that overlaps the
         * given Interval.
         * @param t - the given Interval
         * @return the minimum Node from this Node's subtree that overlaps the
         * Interval t, if one exists; otherwise, the sentinel Node
         */
        private Node minimumOverlappingNode(T t) {

            Node result = nil;
            Node n = this;

            if (!n.isNil() && n.maxEnd > t.start()) {
                while (true) {
                    if (n.overlaps(t)) {

                        // This node overlaps. There may be a lesser overlapper
                        // down the left subtree. No need to consider the right
                        // as all overlappers there will be greater.

                        result = n;
                        n = n.left;

                        if (n.isNil() || n.maxEnd <= t.start()) {
                            // Either no left subtree, or nodes can't overlap.
                            break;
                        }
                    } else {

                        // This node doesn't overlap.
                        // Check the left subtree if an overlapper may be there

                        Node left = n.left;
                        if (!left.isNil() && left.maxEnd > t.start()) {
                            n = left;
                        } else {
                            
                        // Left subtree cannot contain an overlapper. Check the
                        // right sub-tree.
                        
                            if (n.start() >= t.end()) {
                                // Nothing in the right subtree can overlap
                                break;
                            }

                            n = n.right;
                            if (n.isNil() || n.maxEnd <= t.start()) {
                                // No right subtree, or nodes can't overlap.
                                break;
                            }
                        }
                    }
                }
            }

            return result;
        }
        
        /**
         * An Iterator over all values in this Node's subtree that overlap the
         * given Interval t.
         * @param t - the overlapping Interval
         */
        private Iterator<T> overlappers(T t) {
            return new OverlapperIterator(this, t);
        }
        
        /**
         * The next Node (relative to this Node) which overlaps the given
         * Interval t
         * @param t - the overlapping Interval
         * @return the next Node that overlaps the Interval t, if one exists;
         * otherwise, the sentinel Node
         */
        private Node nextOverlappingNode(T t) {
            Node x = this;
            Node rtrn = nil;

            // First, check the right subtree for its minimum overlapper.
            if (!right.isNil()) {
                rtrn = x.right.minimumOverlappingNode(t);
            }
            
            // If we didn't find it in the right subtree, walk up the tree and
            // check the parents of left-children as well as their right subtrees.
            while (!x.parent.isNil() && rtrn.isNil()) {
                if (x.isLeftChild()) {
                    rtrn = x.parent.overlaps(t) ? x.parent
                                                : x.parent.right.minimumOverlappingNode(t);
                }
                x = x.parent;
            }
            return rtrn;
        }
        
        /**
         * The number of Nodes in this Node's subtree that overlap the given
         * Interval t.
         * <p>
         * This number includes this Node if this Node overlaps t. This method
         * iterates over all overlapping Nodes, so if you ultimately need to
         * inspect the Nodes, it will be more efficient to simply create the
         * Iterator yourself.
         * @param t - the overlapping Interval
         * @return the number of overlapping Nodes
         */
        private int numOverlappingIntervals(T t) {
            int count = 0;
            Iterator<Node> iter = new OverlappingNodeIterator(this, t);
            
            while (iter.hasNext()) {
                count += iter.next().intervals.size();
            }
            return count;
        }
        
        //////////////////////////////
        // Node -- Deletion methods //
        //////////////////////////////
        
        //TODO: Should we rewire the Nodes rather than copying data?
        //      I suspect this method causes some code which seems like it
        //      should work to fail.
        
        /**
         * Deletes this Node from its tree.
         * <p>
         * More specifically, removes the data held within this Node from the
         * tree. Depending on the structure of the tree at this Node, this
         * particular Node instance may not be removed; rather, a different
         * Node may be deleted and that Node's contents copied into this one,
         * overwriting the previous contents.
         */
        private boolean delete() {
            
            if (isNil()) {  // Can't delete the sentinel node.
                return false;
            }
            
            size -= this.intervals.size();
            Node y = this;

            if (hasTwoChildren()) { // If the node to remove has two children,
                y = successor();    // copy the successor's data into it and
                copyData(y);        // remove the successor. The successor is
                maxEndFixup();      // guaranteed to both exist and have at most
            }                       // one child, so we've converted the two-
                                    // child case to a one- or no-child case.
            
            
            Node x = y.left.isNil() ? y.right : y.left;

            x.parent = y.parent;

            if (y.isRoot()) {
                root = x;
            } else if (y.isLeftChild()) {
                y.parent.left = x;
                y.maxEndFixup();
            } else {
                y.parent.right = x;
                y.maxEndFixup();
            }
            
            if (y.isBlack) {
                x.deleteFixup();
            }
            
            return true;
        }
        
        ////////////////////////////////////////////////
        // Node -- Tree-invariant maintenance methods //
        ////////////////////////////////////////////////

        /**
         * Whether or not this Node is the root of its tree.
         */
        public boolean isRoot() {
            return (!isNil() && parent.isNil());
        }
        
        /**
         * Whether or not this Node is the sentinel node.
         */
        public boolean isNil() {
            return this == nil;
        }

        /**
         * Whether or not this Node is the left child of its parent.
         */
        public boolean isLeftChild() {
            return this == parent.left;
        }

        /**
         * Whether or not this Node is the right child of its parent.
         */
        public boolean isRightChild() {
            return this == parent.right;
        }

        /**
         * Whether or not this Node has no children, i.e., is a leaf.
         */
        public boolean hasNoChildren() {
            return left.isNil() && right.isNil();
        }

        /**
         * Whether or not this Node has two children, i.e., neither of its
         * children are leaves.
         */
        public boolean hasTwoChildren() {
            return !left.isNil() && !right.isNil();
        }
        
        /**
         * Sets this Node's color to black.
         */
        private void blacken() {
            isBlack = true;
        }
        
        /**
         * Sets this Node's color to red.
         */
        private void redden() {
            isBlack = false;
        }
        
        /**
         * Whether or not this Node's color is red.
         */
        public boolean isRed() {
            return !isBlack;
        }
        
        /**
         * A pointer to the grandparent of this Node.
         */
        private Node grandparent() {
            return parent.parent;
        }

        /**
         * Sets the maxEnd value for this Node.
         * <p>
         * The maxEnd value should be the highest of:
         * <ul>
         * <li>the end value of this node's data
         * <li>the maxEnd value of this node's left child, if not null
         * <li>the maxEnd value of this node's right child, if not null
         * </ul><p>
         * This method will be correct only if the left and right children have
         * correct maxEnd values.
         */
        private void resetMaxEnd() {
            int val = end;
            if (!left.isNil()) {
                val = Math.max(val, left.maxEnd);
            }
            if (!right.isNil()) {
                val = Math.max(val, right.maxEnd);
            }
            maxEnd = val;
        }
        
        /**
         * Sets the maxEnd value for this Node, and all Nodes up to the root of
         * the tree.
         */
        private void maxEndFixup() {
            Node n = this;
            n.resetMaxEnd();
            while (!n.parent.isNil()) {
                n = n.parent;
                n.resetMaxEnd();
            }
        }
        
        /**
         * Performs a left-rotation on this Node.
         * @see - Cormen et al. "Introduction to Algorithms", 2nd ed, pp. 277-279.
         */
        private void leftRotate() {
            Node y = right;
            right = y.left;

            if (!y.left.isNil()) {
                y.left.parent = this;
            }
            
            y.parent = parent;
            
            if (parent.isNil()) {
                root = y;
            } else if (isLeftChild()) {
                parent.left = y;
            } else {
                parent.right = y;
            }
            
            y.left = this;
            parent = y;
            
            resetMaxEnd();
            y.resetMaxEnd();
        }
        
        /**
         * Performs a right-rotation on this Node.
         * @see - Cormen et al. "Introduction to Algorithms", 2nd ed, pp. 277-279.
         */
        private void rightRotate() {
            Node y = left;
            left = y.right;

            if (!y.right.isNil()) {
                y.right.parent = this;
            }
            
            y.parent = parent;
            
            if (parent.isNil()) {
                root = y;
            } else if (isLeftChild()) {
                parent.left = y;
            } else {
                parent.right = y;
            }
            
            y.right = this;
            parent = y;
            
            resetMaxEnd();
            y.resetMaxEnd();
        }

        /**
         * Copies the data from a Node into this Node.
         * @param o - the other Node containing the data to be copied
         */
        private void copyData(Node o) {
            intervals = o.intervals;
        }
        
        @Override
        public String toString() {
            if (isNil()) {
                return "nil";
            } else {
                String color = isBlack ? "black" : "red"; 
                return "start = " + start() +
                       "\nend = " + end() +
                       "\nmaxEnd = " + maxEnd +
                       "\ncolor = " + color;
            }
        }
        
        /**
         * Ensures that red-black constraints and interval-tree constraints are
         * maintained after an insertion.
         */
        private void insertFixup() {
            Node z = this;
            while (z.parent.isRed()) {
                if (z.parent.isLeftChild()) {
                    Node y = z.parent.parent.right;
                    if (y.isRed()) {
                        z.parent.blacken();
                        y.blacken();
                        z.grandparent().redden();
                        z = z.grandparent();
                    } else {
                        if (z.isRightChild()) {
                            z = z.parent;
                            z.leftRotate();
                        }
                        z.parent.blacken();
                        z.grandparent().redden();
                        z.grandparent().rightRotate();
                    }
                } else {
                    Node y = z.grandparent().left;
                    if (y.isRed()) {
                        z.parent.blacken();
                        y.blacken();
                        z.grandparent().redden();
                        z = z.grandparent();
                    } else {
                        if (z.isLeftChild()) {
                            z = z.parent;
                            z.rightRotate();
                        }
                        z.parent.blacken();
                        z.grandparent().redden();
                        z.grandparent().leftRotate();
                    }
                }
            }
            root.blacken();
        }
        
        /**
         * Ensures that red-black constraints and interval-tree constraints are
         * maintained after deletion.
         */
        private void deleteFixup() {
            Node x = this;
            while (!x.isRoot() && x.isBlack) {
                if (x.isLeftChild()) {
                    Node w = x.parent.right;
                    if (w.isRed()) {
                        w.blacken();
                        x.parent.redden();
                        x.parent.leftRotate();
                        w = x.parent.right;
                    }
                    if (w.left.isBlack && w.right.isBlack) {
                        w.redden();
                        x = x.parent;
                    } else {
                        if (w.right.isBlack) {
                            w.left.blacken();
                            w.redden();
                            w.rightRotate();
                            w = x.parent.right;
                        }
                        w.isBlack = x.parent.isBlack;
                        x.parent.blacken();
                        w.right.blacken();
                        x.parent.leftRotate();
                        x = root;
                    }
                } else {
                    Node w = x.parent.left;
                    if (w.isRed()) {
                        w.blacken();
                        x.parent.redden();
                        x.parent.rightRotate();
                        w = x.parent.left;
                    }
                    if (w.left.isBlack && w.right.isBlack) {
                        w.redden();
                        x = x.parent;
                    } else {
                        if (w.left.isBlack) {
                            w.right.blacken();
                            w.redden();
                            w.leftRotate();
                            w = x.parent.left;
                        }
                        w.isBlack = x.parent.isBlack;
                        x.parent.blacken();
                        w.left.blacken();
                        x.parent.rightRotate();
                        x = root;
                    }                    
                }
            }
            x.blacken();
        }
        
        ///////////////////////////////
        // Node -- Debugging methods //
        ///////////////////////////////
        
        /**
         * Whether or not the subtree rooted at this Node is a valid
         * binary-search tree.
         * @param min - a lower-bound Node
         * @param max - an upper-bound Node
         */
        private boolean isBST(Node min, Node max) {
            if (isNil()) {
                return true;   // Leaves are a valid BST, trivially.
            }
            if (min != null && compareTo(min) <= 0) {
                return false;  // This Node must be greater than min
            }
            if (max != null && compareTo(max) >= 0) {
                return false;  // and less than max.
            }
            
            // Children recursively call method with updated min/max.
            return left.isBST(min, this) && right.isBST(this, max);
        }
        
        /**
         * Whether or not the subtree rooted at this Node is balanced.
         * <p>
         * Balance determination is done by calculating the black-height.
         * @param black - the expected black-height of this subtree
         */
        private boolean isBalanced(int black) {
            if (isNil()) {
                return black == 0;  // Leaves have a black-height of zero,
            }                       // even though they are black.
            if (isBlack) {
                black--;
            }
            return left.isBalanced(black) && right.isBalanced(black);
        }
        
        /**
         * Whether or not the subtree rooted at this Node has a valid
         * red-coloring.
         * <p>
         * A red-black tree has a valid red-coloring if every red node has two
         * black children.
         */
        private boolean hasValidRedColoring() {
            if (isNil()) {
                return true;
            } else if (isBlack) {
                return left.hasValidRedColoring() &&
                        right.hasValidRedColoring();
            } else {
                return left.isBlack && right.isBlack &&
                        left.hasValidRedColoring() &&
                        right.hasValidRedColoring();
            }
        }
        
        /**
         * Whether or not the subtree rooted at this Node has consistent maxEnd
         * values.
         * <p>
         * The maxEnd value of an interval-tree Node is equal to the maximum of
         * the end-values of all intervals contained in the Node's subtree.
         */
        private boolean hasConsistentMaxEnds() {

            if (isNil()) {                                    // 1. sentinel node
                return true;
            }
            
            if (hasNoChildren()) {                            // 2. leaf node
                return maxEnd == end();
            } else {
                boolean consistent = maxEnd >= end();
                if (hasTwoChildren()) {                       // 3. two children
                    return consistent &&
                           maxEnd >= left.maxEnd &&
                           maxEnd >= right.maxEnd &&
                           left.hasConsistentMaxEnds() &&
                           right.hasConsistentMaxEnds();
                } else if (left.isNil()) {                    // 4. one child -- right
                    return consistent &&
                           maxEnd >= right.maxEnd &&
                           right.hasConsistentMaxEnds();
                } else {
                    return consistent &&                      // 5. one child -- left
                           maxEnd >= left.maxEnd &&
                           left.hasConsistentMaxEnds();
                }
            }
        }
    }
    
    ///////////////////////
    // Tree -- Iterators //
    ///////////////////////
    
    /**
     * An Iterator which walks along this IntervalTree's Nodes in ascending order.
     */
    @SuppressWarnings("unused")
    private class TreeNodeIterator implements Iterator<Node> {

        private Node next;
        
        private TreeNodeIterator(Node root) {
            next = root.minimumNode();
        }
        
        @Override
        public boolean hasNext() {
            return !next.isNil();
        }

        @Override 
        public Node next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Interval tree has no more elements.");
            }
            Node rtrn = next;
            next = rtrn.successor();
            return rtrn;
        }   
    }

    /**
     * An Iterator which walks along this IntervalTree's Intervals in ascending
     * order.
     */
    private class TreeIterator implements Iterator<T> {
        
        private Iterator<T> iter;
        private Node currentNode;
        private Node nextNode;
        
        private TreeIterator(Node root) {
            currentNode = root.minimumNode();
            nextNode = currentNode.successor();
            iter = currentNode.iterator();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext() || !nextNode.isNil();
        }

        @Override
        public T next() {
            if (iter.hasNext()) {
                return iter.next();
            } else {
                currentNode = nextNode;
                nextNode = currentNode.successor();
                iter = currentNode.iterator();
                return iter.next();
            }
        }
    }
 
    /**
     * An Iterator which walks along this IntervalTree's Nodes that overlap
     * a given Interval in ascending order.
     */
    private class OverlappingNodeIterator implements Iterator<Node> {
        
        private Node next;
        private T interval;
        
        private OverlappingNodeIterator(Node root, T t) {
            interval = t;
            next = root.minimumOverlappingNode(interval);
        }
        
        @Override
        public boolean hasNext() {
            return !next.isNil();
        }
        
        @Override
        public Node next() {
            if (!hasNext()) {
                throw new NoSuchElementException("Interval tree has no more overlapping elements.");
            }
            Node rtrn = next;
            next = rtrn.nextOverlappingNode(interval);
            return rtrn;
        }
    }

    /**
     * An Iterator which walks along this IntervalTree's Intervals that overlap
     * a given Interval in ascending order.
     * <p>
     * This class just wraps an OverlappingNodeIterator and extracts each Node's
     * Interval.
     */
    private class OverlapperIterator implements Iterator<T> {
        
        private Iterator<T> iter;
        private Node currentNode;
        private Node nextNode;
        private T t;
        
        private OverlapperIterator(Node root, T t) {
            this.t = t;
            currentNode = root.minimumOverlappingNode(t);
            nextNode = currentNode.nextOverlappingNode(t);
            iter = currentNode.iterator();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext() || !nextNode.isNil();
        }

        @Override
        public T next() {
            if (iter.hasNext()) {
                return iter.next();
            } else {
                currentNode = nextNode;
                nextNode = currentNode.nextOverlappingNode(t);
                iter = currentNode.iterator();
                return iter.next();
            }
        }
    }

    ///////////////////////////////
    // Tree -- Debugging methods //
    ///////////////////////////////
    
    /**
     * Whether or not this IntervalTree is a valid binary-search tree.
     * <p>
     * This method will return false if any Node is less than its left child
     * or greater than its right child.
     * <p>
     * This method is used for debugging only, and its access is changed in
     * testing.
     */
    @SuppressWarnings("unused")
    private boolean isBST() {
        return root.isBST(null, null);
    }

    /**
     * Whether or not this IntervalTree is balanced.
     * <p>
     * This method will return false if all of the branches (from root to leaf)
     * do not contain the same number of black nodes. (Specifically, the
     * black-number of each branch is compared against the black-number of the
     * left-most branch.)
     * <p>
     * This method is used for debugging only, and its access is changed in
     * testing.
     */
    @SuppressWarnings("unused")
    private boolean isBalanced() { 
        int black = 0;
        Node x = root;
        while (!x.isNil()) {
            if (x.isBlack) {
                black++;
            }
            x = x.left;
        }
        return root.isBalanced(black);
    }
    
    /**
     * Whether or not this IntervalTree has a valid red coloring.
     * <p>
     * This method will return false if all of the branches (from root to leaf)
     * do not contain the same number of black nodes. (Specifically, the
     * black-number of each branch is compared against the black-number of the
     * left-most branch.)
     * <p>
     * This method is used for debugging only, and its access is changed in
     * testing.
     */
    @SuppressWarnings("unused")
    private boolean hasValidRedColoring() {
        return root.hasValidRedColoring();
    }
    
    /**
     * Whether or not this IntervalTree has consistent maxEnd values.
     * <p>
     * This method will only return true if each Node has a maxEnd value equal
     * to the highest interval end value of all the intervals in its subtree.
     * <p>
     * This method is used for debugging only, and its access is changed in
     * testing.
     */
    @SuppressWarnings("unused")
    private boolean hasConsistentMaxEnds() {
        return root.hasConsistentMaxEnds();
    }
}