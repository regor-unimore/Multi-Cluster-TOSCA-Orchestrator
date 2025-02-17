def limited_deep_merge(dict1, dict2, max_depth=10):
    """Deeply merges two dictionaries, limiting the maximum depth.

    Args:
        dict1 (dict): The first dictionary.
        dict2 (dict): The second dictionary.
        max_depth (int, optional): The maximum depth to merge. If None, it is set to 1. Limit is 100.

    Returns:
        dict: The merged dictionary.
    """
    if max_depth > 100:
       raise ValueError("max_depth must be lower than 100 for this function")
    if not isinstance(dict1, dict) or not isinstance(dict2, dict):
       raise TypeError("dict1 and dict2 must be dictionaries!")
    
    merged_dict = dict1.copy()

    def merge_recursive(dict1, dict2, depth=0):
        if depth >= max_depth:
            return

        for key, value in dict2.items():
            if isinstance(value, dict) and isinstance(dict1.get(key), dict):
                merge_recursive(dict1[key], value, depth + 1)
            elif isinstance(value, list) and isinstance(dict1.get(key), list):
                dict1[key] = list( set(dict1.get(key)) | set(value) )
            elif isinstance(dict1.get(key), list):
                if value not in dict1[key]:
                    dict1[key].append(value)
            #overwrite the value if key conflicts!
            else:
                dict1[key] = value

    merge_recursive(merged_dict, dict2)
    return merged_dict


