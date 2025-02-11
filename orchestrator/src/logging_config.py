import logging


def configure_logger():
    logging.basicConfig(
        filename='solver.log',
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    logger = logging.getLogger()
    file_handler = logging.FileHandler('solver.log')
    console_handler = logging.StreamHandler()

    if not logger.handlers:  # Check if handlers already exist
        # Configure logging file
        file_handler.setLevel(logging.INFO)
        file_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))
        logger.addHandler(file_handler)
        
        # Configure logging console
        console_handler.setLevel(logging.INFO)
        console_handler.setFormatter(logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s'))
        logger.addHandler(console_handler)

        logger.setLevel(logging.INFO)  # Set the main logger level

    # Configure Flask/Werkzeug logger
    werkzeug_logger = logging.getLogger('werkzeug')
    if not werkzeug_logger.handlers:  # Check if handlers already exist
        werkzeug_logger.addHandler(console_handler)
        werkzeug_logger.setLevel(logging.INFO)

    return logger

