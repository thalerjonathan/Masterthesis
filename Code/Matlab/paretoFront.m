function [ output_args ] = paretoFront( data, t )
%PARETOFRONT Summary of this function goes here
%   Detailed explanation goes here

sortedData = sort( data );

assetPrice = sortedData( :, 1 );
loanPrice = sortedData( : , 2 );

plot( assetPrice, loanPrice, '-X' );
title( t );
legend( 'Pareto frontier' );
xlabel( 'Asset Price' );
ylabel( 'Loan Price' );

end

