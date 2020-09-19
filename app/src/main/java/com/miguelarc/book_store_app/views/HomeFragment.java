package com.miguelarc.book_store_app.views;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.miguelarc.book_store_app.database.FavoriteBooksDatabase;
import com.miguelarc.book_store_app.FragmentNavigationHandler;
import com.miguelarc.book_store_app.RecyclerViewClickListener;
import com.miguelarc.book_store_app.adapters.BookListAdapter;
import com.miguelarc.book_store_app.R;
import com.miguelarc.book_store_app.models.Book;
import com.miguelarc.book_store_app.network.responsemodels.BookListResponse;
import com.miguelarc.book_store_app.viewmodels.HomeViewModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private RecyclerView bookListRecyclerView;
    private BookListAdapter bookListAdapter;
    private static final int PAGE_SIZE = 20;
    private ProgressBar progressBar;
    private boolean hasReachedEnd = false;
    private CheckBox favoriteCheckBox;
    private FavoriteBooksDatabase favoriteBooksDatabase;
    private RecyclerViewClickListener listener = new RecyclerViewClickListener() {
        @Override
        public void onItemClicked(Book book) {
            onBookClicked(book);
        }
    };
    private CompoundButton.OnCheckedChangeListener favoriteCheckListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            onFavoriteCheckClicked(isChecked);
        }
    };

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        progressBar = rootView.findViewById(R.id.main_progress);
        favoriteCheckBox = rootView.findViewById(R.id.favorites_check_box);
        favoriteCheckBox.setOnCheckedChangeListener(favoriteCheckListener);
        favoriteBooksDatabase = FavoriteBooksDatabase.getInstance(this.getContext());

        initRecyclerView(rootView);
        initScrollListener();

        // Loading initial batch of books
        loadInitialBooks();

        return rootView;
    }

    private void initRecyclerView(View rootView) {
        bookListRecyclerView = rootView.findViewById(R.id.book_list_recycler_view);
        bookListRecyclerView.setLayoutManager(new GridLayoutManager(this.getContext(), 2));
        bookListRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void initScrollListener() {
        bookListRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager gridLayoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (gridLayoutManager != null && gridLayoutManager.findLastCompletelyVisibleItemPosition() == bookListAdapter.getItemCount() - 1) {
                    if (!hasReachedEnd) {
                        progressBar.setVisibility(View.VISIBLE);
                        loadNextBooks();
                    }
                }
            }
        });
    }

    private void loadInitialBooks() {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.getInitialBookList().observe(getViewLifecycleOwner(), new Observer<BookListResponse>() {
            @Override
            public void onChanged(BookListResponse bookListResponse) {
                if (bookListResponse.getItems().size() < PAGE_SIZE) {
                    // Reached end of list. App shouldn't be requesting more items.
                    hasReachedEnd = true;
                }
                if (bookListAdapter == null) {
                    bookListAdapter = new BookListAdapter(bookListResponse.getItems(), listener);
                } else {
                    bookListAdapter.addBookItems(bookListResponse.getItems());
                }
                bookListRecyclerView.setAdapter(bookListAdapter);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadNextBooks() {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        homeViewModel.getNextBookList().observe(getViewLifecycleOwner(), new Observer<BookListResponse>() {
            @Override
            public void onChanged(BookListResponse bookListResponse) {
                if (bookListResponse.getItems().size() < PAGE_SIZE) {
                    // Reached end of list. App shouldn't be requesting more items.
                    hasReachedEnd = true;
                }
                if (bookListAdapter == null) {
                    bookListAdapter = new BookListAdapter(bookListResponse.getItems(), listener);
                } else {
                    bookListAdapter.addBookItems(bookListResponse.getItems());
                }
                bookListRecyclerView.setAdapter(bookListAdapter);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void onBookClicked(Book clickedBook) {
        BookDetailsFragment bookDetailsFragment = BookDetailsFragment.newInstance(clickedBook);
        if (this.getActivity() != null) {
            ((FragmentNavigationHandler)this.getActivity()).pushFragment(bookDetailsFragment);
        }
    }

    private void onFavoriteCheckClicked(boolean isChecked) {
        if (isChecked) {
            favoriteBooksDatabase.bookDao().loadFavoriteBooks().observe(getViewLifecycleOwner(), new Observer<List<Book>>() {
                @Override
                public void onChanged(List<Book> favoriteBookList) {
                    clearBookList();
                    bookListAdapter.addBookItems(favoriteBookList);
                }
            });
        } else {
            loadInitialBooks();
        }
    }

    private void clearBookList() {
        bookListAdapter.clearBookItems();
    }
}