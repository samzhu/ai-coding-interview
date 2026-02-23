class TodoStore:
    """A simple in-memory TODO list manager."""

    def __init__(self):
        self._items = []
        self._next_id = 1

    def add(self, title: str) -> dict:
        """Add a new todo item.

        Args:
            title: The title of the todo item.

        Returns:
            A dict with keys: id (int), title (str), done (bool)
        """
        # TODO: Implement add()
        # 1. Create item dict with id, title, done=False
        # 2. Append to self._items
        # 3. Increment self._next_id
        # 4. Return the created item
        pass

    def get_all(self) -> list:
        """Return all todo items as a list of dicts."""
        # TODO: Implement get_all()
        pass

    def mark_done(self, item_id: int) -> dict:
        """Mark a todo item as done.

        Args:
            item_id: The id of the item to mark as done.

        Returns:
            The updated item dict.

        Raises:
            ValueError: If no item with the given id exists.
        """
        # TODO: Implement mark_done()
        # 1. Find item with matching id
        # 2. If not found, raise ValueError(f"Item {item_id} not found")
        # 3. Set done=True on the item
        # 4. Return the updated item
        pass

    def get_pending(self) -> list:
        """Return only items where done is False."""
        # TODO: Implement get_pending()
        pass
