"""Task scheduler using topological sort."""
import heapq
from graph import build_graph


def topological_sort(tasks: list, dependencies: list) -> list:
    """Find a valid execution order for tasks respecting dependencies and priority.

    Uses Kahn's algorithm (BFS-based topological sort) with a min-heap
    to prioritize tasks by their priority value (lower value = higher priority).

    Args:
        tasks: List of dicts with 'id' (str) and 'priority' (int) fields.
        dependencies: List of (depender, dependee) tuples.
                     depender depends on dependee (dependee runs first).

    Returns:
        List of task IDs in valid execution order.

    Raises:
        ValueError: If a circular dependency is detected.
    """
    if not tasks:
        return []

    # TODO: Implement Kahn's algorithm with priority queue
    # 1. Build the graph using build_graph(tasks, dependencies)
    # 2. Create a priority lookup: {task_id: priority}
    # 3. Initialize a min-heap with all tasks that have in_degree == 0
    #    Heap entry format: (priority, task_id)
    # 4. Process the heap:
    #    a. Pop the task with lowest priority value (heapq.heappop)
    #    b. Add its id to the result list
    #    c. For each neighbor in adj[task_id]:
    #       - Decrement in_degree[neighbor]
    #       - If in_degree[neighbor] == 0, push to heap
    # 5. If len(result) < len(tasks): raise ValueError("Cycle detected")
    # 6. Return result

    return []
