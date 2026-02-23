"""Dependency graph builder for the task scheduler."""


def build_graph(tasks: list, dependencies: list) -> dict:
    """Build an adjacency list and in-degree map from task dependencies.

    Args:
        tasks: List of task dicts with 'id' and 'priority' fields.
        dependencies: List of (depender, dependee) tuples.
                     Each tuple means: depender depends on dependee
                     i.e., dependee must run BEFORE depender.

    Returns:
        A dict with:
            'adj': dict mapping task_id -> list of task_ids that depend on it
            'in_degree': dict mapping task_id -> number of dependencies

    BUG: The dependency direction is reversed.
    If task A depends_on B, the edge should be B -> A (B must run before A).
    Currently the code adds edge A -> B (wrong direction).
    """
    adj = {task['id']: [] for task in tasks}
    in_degree = {task['id']: 0 for task in tasks}

    for depender, dependee in dependencies:
        # BUG: direction is wrong — should be adj[dependee].append(depender)
        # and in_degree[depender] += 1
        adj[depender].append(dependee)  # BUG: wrong direction
        in_degree[dependee] += 1        # BUG: wrong in_degree update

    return {'adj': adj, 'in_degree': in_degree}
