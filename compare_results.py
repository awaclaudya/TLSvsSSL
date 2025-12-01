import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os

# --- Data Loading ---
def load_data():
    """
    Loads SSL and TLS performance data from their respective subdirectories
    based on the project structure.
    """
    # Define the correct paths to the data files
    ssl_file = 'ssl3/ssl_results.csv'
    tls_file = 'tls13/tls13_results.csv'

    # Define the column names since the CSV files don't have a header
    column_names = ['Protocol', 'FileSizeMB', 'Duration', 'ThroughputMBps', 'Iteration']

    # Check if the files exist before trying to read them
    if not os.path.exists(ssl_file):
        print(f"Error: Could not find the SSL data file at '{ssl_file}'")
        return None
    if not os.path.exists(tls_file):
        print(f"Error: Could not find the TLS data file at '{tls_file}'")
        return None

    # Load data into pandas DataFrames, providing the column names
    try:
        # header=None tells pandas there is no header row in the file
        # names=column_names assigns the correct names to the columns
        ssl_df = pd.read_csv(ssl_file, header=None, names=column_names)
        tls_df = pd.read_csv(tls_file, header=None, names=column_names)
    except Exception as e:
        print(f"Error reading CSV files: {e}")
        return None

    # Combine the two dataframes for easy analysis
    combined_df = pd.concat([ssl_df, tls_df], ignore_index=True)
    
    return combined_df

# --- Data Processing and Plotting ---
def create_graphs(df):
    """
    Processes the data and generates comparison graphs.
    """
    # Set a nice style for the plots
    sns.set_theme(style="whitegrid")

    # --- Graph 1: Average Throughput vs. File Size ---
    plt.figure(figsize=(12, 7))
    sns.lineplot(
        data=df, 
        x='FileSizeMB', 
        y='ThroughputMBps', 
        hue='Protocol', 
        marker='o', 
        dashes=False
    )
    plt.title('Average Throughput vs. File Size', fontsize=16)
    plt.xlabel('File Size (MB)', fontsize=12)
    plt.ylabel('Throughput (MB/s)', fontsize=12)
    plt.xscale('log') # Use log scale for file size to better see small values
    plt.xticks([1, 5, 10, 1024], ['1', '5', '10', '1024'])
    plt.legend(title='Protocol')
    plt.tight_layout()
    plt.savefig('throughput_comparison.png')
    plt.show()

    # --- Graph 2: Average Duration vs. File Size ---
    plt.figure(figsize=(12, 7))
    sns.lineplot(
        data=df, 
        x='FileSizeMB', 
        y='Duration', 
        hue='Protocol', 
        marker='o', 
        dashes=False
    )
    plt.title('Average Duration vs. File Size', fontsize=16)
    plt.xlabel('File Size (MB)', fontsize=12)
    plt.ylabel('Duration (ms)', fontsize=12)
    plt.xscale('log') # Use log scale for file size
    plt.xticks([1, 5, 10, 1024], ['1', '5', '10', '1024'])
    plt.legend(title='Protocol')
    plt.tight_layout()
    plt.savefig('duration_comparison.png')
    plt.show()

    # --- Graph 3: Throughput vs. File Size with Error Bars (Standard Deviation) ---
    plt.figure(figsize=(12, 7))
    sns.lineplot(
        data=df, 
        x='FileSizeMB', 
        y='ThroughputMBps', 
        hue='Protocol', 
        marker='o',
        err_style='bars', # Show error bars
        ci='sd'           # Confidence interval as standard deviation
    )
    plt.title('Throughput vs. File Size (with Standard Deviation)', fontsize=16)
    plt.xlabel('File Size (MB)', fontsize=12)
    plt.ylabel('Throughput (MB/s)', fontsize=12)
    plt.xscale('log') # Use log scale for file size
    plt.xticks([1, 5, 10, 1024], ['1', '5', '10', '1024'])
    plt.legend(title='Protocol')
    plt.tight_layout()
    plt.savefig('throughput_with_error_bars.png')
    plt.show()


# --- Main Execution ---
if __name__ == "__main__":
    performance_data = load_data()
    if performance_data is not None:
        create_graphs(performance_data)
        print("\nGraphs generated successfully!")
        print("Files saved: throughput_comparison.png, duration_comparison.png, throughput_with_error_bars.png")